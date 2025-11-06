package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.dto.post.PostStatResponseDto;
import com.kakaotechbootcamp.community.entity.Post;
import com.kakaotechbootcamp.community.entity.PostStat;
import com.kakaotechbootcamp.community.exception.NotFoundException;
import com.kakaotechbootcamp.community.repository.CommentRepository;
import com.kakaotechbootcamp.community.repository.PostLikeRepository;
import com.kakaotechbootcamp.community.repository.PostRepository;
import com.kakaotechbootcamp.community.repository.PostStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 통계 서비스
 * - 의도: 통계 조회/동기화(좋아요/댓글 수) 제공
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostStatService {

    private final PostRepository postRepository;
    private final PostStatRepository postStatRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;

    /**
     * 통계 생성
     * - 의도: Post 생성 시 PostStat도 함께 생성
     */
    @Transactional
    public PostStat create(Post post) {
        return postStatRepository.save(new PostStat(post));
    }

    /**
     * 통계 조회 또는 생성 (헬퍼 메서드)
     * - 의도: PostStat가 없으면 생성 (레거시 데이터 대응)
     * - 주의: Post 생성 시 PostStat도 함께 생성되므로 일반적으로 없을 수 없음
     */
    @Transactional
    public PostStat findByIdOrCreate(Integer postId) {
        return postStatRepository.findById(postId)
                .orElseGet(() -> {
                    Post post = postRepository.findById(postId)
                            .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다"));
                    return postStatRepository.save(new PostStat(post));
                });
    }

    /**
     * 통계 조회
     * - 에러: 미존재 시 404
     */
    public ApiResponse<PostStatResponseDto> getByPostId(Integer postId) {
        PostStat stat = postStatRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("통계를 찾을 수 없습니다"));
        return ApiResponse.modified(PostStatResponseDto.from(stat));
    }

    /**
     * 통계 동기화
     * - 의도: 테이블(PostLike, Comment)로부터 like/comment 수 집계하여 PostStat 반영
     */
    @Transactional
    public PostStat syncStatistics(Integer postId) {
        PostStat stat = findByIdOrCreate(postId);

        int likeCount = postLikeRepository.countByIdPostId(postId);
        int commentCount = commentRepository.countByPostId(postId);

        stat.syncLikeCount(likeCount);
        stat.syncCommentCount(commentCount);

        return stat;
    }

    /**
     * 통계 동기화 (API 응답용)
     * - 의도: 테이블(PostLike, Comment)로부터 like/comment 수 집계하여 PostStat 반영 및 저장
     */
    @Transactional
    public ApiResponse<PostStatResponseDto> syncAll(Integer postId) {
        PostStat stat = syncStatistics(postId);
        PostStat saved = postStatRepository.save(stat);
        return ApiResponse.modified(PostStatResponseDto.from(saved));
    }
}
