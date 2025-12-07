package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.common.ImageType;
import com.kakaotechbootcamp.community.common.ImageProperties;
import com.kakaotechbootcamp.community.dto.post.*;
import com.kakaotechbootcamp.community.entity.*;
import com.kakaotechbootcamp.community.exception.NotFoundException;
import com.kakaotechbootcamp.community.exception.BadRequestException;
import com.kakaotechbootcamp.community.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 게시글(Post) 도메인 서비스
 * - 목록/상세 조회, 생성, 수정 비즈니스 로직
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostStatRepository postStatRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostStatAsyncService postStatAsyncService;
    private final PostStatService postStatService;
    private final ImageUploadService imageUploadService;
    private final ImageProperties imageProperties;

    /**
     * 게시글 목록 조회(커서 기반)
     * - 의도: id 내림차순 커서 페이지네이션
     * - 반환: items, nextCursor, hasNext
     */
    @Transactional(readOnly = true)
    public ApiResponse<PostResponseDto> list(Integer cursor, Integer size, Integer currentUserId) {
        int requested = (size == null) ? 10 : size;
        int pageSize = requested <= 0 ? 10 : Math.min(requested, 20);
        Pageable pageable = PageRequest.of(0, pageSize);

        List<Post> posts;
        if (cursor == null || cursor <= 0) {
            posts = postRepository.findFirstPageWithUser(pageable);
        } else {
            posts = postRepository.findPageByCursorWithUser(cursor, pageable);
        }

        List<Integer> postIds = posts.stream().map(Post::getId).toList();
        Map<Integer, PostStat> postIdToStat = new HashMap<>();
        if (!postIds.isEmpty()) {
            postStatRepository.findAllById(postIds).forEach(stat -> postIdToStat.put(stat.getId(), stat));
            
            // 각 게시글의 통계를 실제 DB에서 동기화
            for (Integer postId : postIds) {
                PostStat stat = postIdToStat.get(postId);
                if (stat == null) {
                    continue;
                }
                // PostStatService를 통해 통계 동기화 후 Map에 반영
                PostStat syncedStat = postStatService.syncStatistics(postId);
                postIdToStat.put(postId, syncedStat);
            }
        }

        // 좋아요 일괄 조회
        Map<Integer, Boolean> postIdToIsLiked = new HashMap<>();
        if (currentUserId != null && !postIds.isEmpty()) {
            List<PostLike> likes = postLikeRepository.findByIdPostIdInAndIdUserId(postIds, currentUserId);
            likes.forEach(like -> postIdToIsLiked.put(like.getId().getPostId(), true));
        }

        List<PostListItemDto> items = PostListItemDto.from(posts, postIdToStat, postIdToIsLiked);
        Integer nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).postId();
        boolean hasNext = items.size() == pageSize;

        return ApiResponse.success(new PostResponseDto(items, nextCursor, hasNext));
    }

    /**
     * 게시글 상세 조회
     * - 의도: 조회수 +1, 이미지/댓글/통계 포함해 반환
     * - 에러: 게시글 미존재 시 404
     */
    @Transactional
    public ApiResponse<PostDetailDto> getDetail(Integer postId, Integer currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다"));

        List<PostImage> images = postImageRepository.findByPostIdOrderByDisplayOrderAsc(postId);
        PostStat stat = postStatService.findByIdOrCreate(postId);

        // 조회수 증가: 비동기 처리
        postStatAsyncService.incrementViewCount(postId);

        // 댓글 + 작성자
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAscWithUser(postId);

        // 통계 동기화
        PostStat syncedStat = postStatService.syncStatistics(postId);
        
        // 응답용 통계 객체 생성 (viewCount는 DB 값 + 1)
        PostStat responseStat = new PostStat(post);
        responseStat.syncLikeCount(syncedStat.getLikeCount());
        responseStat.syncCommentCount(syncedStat.getCommentCount());
        responseStat.syncViewCount(stat.getViewCount() + 1);
        
        boolean isLiked = (currentUserId != null) && postLikeRepository.existsByIdPostIdAndIdUserId(postId, currentUserId);

        return ApiResponse.success(PostDetailDto.from(post, images, PostStatResponseDto.from(responseStat), comments, isLiked));
    }

    /**
     * 게시글 생성
     * - 의도: 작성자 검증 후 저장, 이미지 순서대로 저장, 통계 초기화
     * - 에러: 작성자 미존재 시 404
     */
    @Transactional
    public ApiResponse<PostDetailDto> create(PostCreateRequestDto request, Integer currentUserId) {
        User author = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("작성자를 찾을 수 없습니다"));

        Post post = new Post(author, request.getTitle().trim(), request.getContent().trim());
        Post saved = postRepository.save(post);

        // 이미지 저장 (displayOrder = index)
        if (request.getImageObjectKeys() != null && !request.getImageObjectKeys().isEmpty()) {
            if (request.getImageObjectKeys().size() > imageProperties.getMaxPerPost()) {
                throw new BadRequestException("이미지 최대 업로드 개수는 " + imageProperties.getMaxPerPost() + "개 입니다");
            }
            // 이미지 objectKey 검증
            List<String> keys = request.getImageObjectKeys();
            for (String objectKey : keys) {
                imageUploadService.validateObjectKey(ImageType.POST, objectKey, saved.getId());
            }
            
            List<PostImage> images = new java.util.ArrayList<>();
            for (int i = 0; i < keys.size(); i++) {
                images.add(new PostImage(saved, keys.get(i), i));
            }
            postImageRepository.saveAll(images);
        }

        // 통계 생성 및 동기화
        PostStat stat = postStatService.create(saved);
        stat = postStatService.syncStatistics(saved.getId());
        
        boolean isLiked = (currentUserId != null) && postLikeRepository.existsByIdPostIdAndIdUserId(saved.getId(), currentUserId);

        return ApiResponse.created(PostDetailDto.from(saved,
                postImageRepository.findByPostIdOrderByDisplayOrderAsc(saved.getId()),
                PostStatResponseDto.from(stat),
                List.of(),
                isLiked));
    }

    /**
     * 게시글 수정
     * - 의도: 제목/내용 선택 수정, 이미지 배열 전달 시 전체 교체
     * - 정책: null=미변경, 빈 배열=전부 제거
     * - 에러: 게시글 미존재 시 404
     */
    @Transactional
    public ApiResponse<PostDetailDto> update(Integer postId, PostUpdateRequestDto request, Integer currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다"));

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            post.updateTitle(request.getTitle().trim());
        }
        if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
            post.updateContent(request.getContent().trim());
        }

        // 이미지 전체 교체 정책
        if (request.getImageObjectKeys() != null) {
            List<PostImage> existing = postImageRepository.findByPostOrderByDisplayOrderAsc(post);
            postImageRepository.deleteAll(existing);

            List<String> keys = request.getImageObjectKeys();
            if (!keys.isEmpty()) {
                if (keys.size() > imageProperties.getMaxPerPost()) {
                    throw new BadRequestException("이미지 최대 업로드 개수는 " + imageProperties.getMaxPerPost() + "개 입니다");
                }
                // 이미지 objectKey 검증
                for (String objectKey : keys) {
                    imageUploadService.validateObjectKey(ImageType.POST, objectKey, postId);
                }
                
                List<PostImage> newImages = new java.util.ArrayList<>(keys.size());
                for (int i = 0; i < keys.size(); i++) {
                    newImages.add(new PostImage(post, keys.get(i), i));
                }
                postImageRepository.saveAll(newImages);
            }
        }

        // 통계 동기화
        PostStat stat = postStatService.syncStatistics(postId);
        boolean isLiked = (currentUserId != null) && postLikeRepository.existsByIdPostIdAndIdUserId(postId, currentUserId);

        return ApiResponse.modified(PostDetailDto.from(post,
                postImageRepository.findByPostIdOrderByDisplayOrderAsc(postId),
                PostStatResponseDto.from(stat),
                commentRepository.findByPostIdOrderByCreatedAtAsc(postId),
                isLiked));
    }

    /**
     * 게시글 삭제(소프트 삭제)
     * - 의도: deletedAt 설정으로 비활성화, 연관 데이터는 유지
     * - 에러: 게시글 미존재 시 404
     */
    @Transactional
    public ApiResponse<Void> delete(Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다"));
        post.softDelete();
        return ApiResponse.deleted(null);
    }
}
