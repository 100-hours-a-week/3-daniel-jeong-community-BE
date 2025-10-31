package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.dto.comment.*;
import com.kakaotechbootcamp.community.entity.Comment;
import com.kakaotechbootcamp.community.entity.Post;
import com.kakaotechbootcamp.community.entity.User;
import com.kakaotechbootcamp.community.exception.BadRequestException;
import com.kakaotechbootcamp.community.exception.NotFoundException;
import com.kakaotechbootcamp.community.repository.CommentRepository;
import com.kakaotechbootcamp.community.repository.PostRepository;
import com.kakaotechbootcamp.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 댓글(Comment) 도메인 서비스
 * - 역할: 댓글 목록/생성/수정/삭제 비즈니스 로직 수행
 * - 검증: 리소스 존재, 부모-자식 정합성, 내용 유효성
 * - 통계: 생성/삭제 시 댓글수 비동기 증감 처리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostStatAsyncService postStatAsyncService;

    /**
     * 댓글 목록 조회 (게시글 기준)
     * - 의도: 생성일 오름차순으로 작성자 정보 포함 반환
     * - 에러: 게시글 미존재 시 404
     */
    public ApiResponse<List<CommentResponseDto>> listByPost(Integer postId) {
        if (postId == null || postId <= 0) {
            throw new BadRequestException("유효한 게시글 ID가 필요합니다");
        }
        if (!postRepository.existsById(postId)) {
            throw new NotFoundException("게시글을 찾을 수 없습니다");
        }
        List<CommentResponseDto> items = commentRepository.findByPostIdOrderByCreatedAtAscWithUser(postId)
                .stream()
                .map(CommentResponseDto::from)
                .toList();
        return ApiResponse.modified(items);
    }

    /**
     * 댓글 생성
     * - 의도: 게시글/사용자 존재 확인 후 댓글 저장, parentId 전달 시 대댓글로 처리
     * - 정책: 내용 공백 불가, 부모는 동일 게시글에 속해야 함, 삭제된 부모 금지, 최대 깊이=2(루트=0, 대댓글=1)
     */
    @Transactional
    public ApiResponse<CommentResponseDto> create(Integer postId, Integer userId, CommentRequestDto request) {
        if (postId == null || postId <= 0 || userId == null || userId <= 0) {
            throw new BadRequestException("유효한 ID가 필요합니다");
        }
        Post post = postRepository.findById(postId).orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다"));
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new BadRequestException("댓글 내용을 입력해주세요");
        }

        Integer parentId = null;
        int depth = 0;
        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("부모 댓글을 찾을 수 없습니다"));
            if (!parent.getPost().getId().equals(postId)) {
                throw new BadRequestException("부모 댓글이 해당 게시글에 속하지 않습니다");
            }
            if (parent.getDeletedAt() != null) {
                throw new BadRequestException("삭제된 댓글에는 답글을 달 수 없습니다");
            }
            if (parent.getDepth() >= 1) { // 최대 깊이: 2
                throw new BadRequestException("대댓글의 하위에는 더 이상 답글을 달 수 없습니다");
            }
            parentId = parent.getId();
            depth = parent.getDepth() + 1;
        }

        Comment saved = commentRepository.save(new Comment(post, user, parentId, request.getContent().trim(), depth));

        // 비동기 댓글수 +1
        postStatAsyncService.incrementCommentCount(postId);

        return ApiResponse.created(CommentResponseDto.from(saved));
    }

    /**
     * 댓글 수정
     * - 의도: 내용만 수정
     * - 에러: 댓글 미존재/내용 공백 시 400/404
     */
    @Transactional
    public ApiResponse<CommentResponseDto> update(Integer commentId, String content) {
        if (commentId == null || commentId <= 0) {
            throw new BadRequestException("유효한 댓글 ID가 필요합니다");
        }
        if (content == null || content.isBlank()) {
            throw new BadRequestException("댓글 내용을 입력해주세요");
        }
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다"));
        comment.updateContent(content.trim());
        Comment saved = commentRepository.save(comment);
        return ApiResponse.modified(CommentResponseDto.from(saved));
    }

    /**
     * 댓글 삭제
     * - 의도: deletedAt 설정, 데이터 보관, 내용 마스킹("삭제된 댓글입니다")
     */
    @Transactional
    public ApiResponse<Void> delete(Integer commentId) {
        if (commentId == null || commentId <= 0) {
            throw new BadRequestException("유효한 댓글 ID가 필요합니다");
        }
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다"));
        Integer postId = comment.getPost().getId();
        comment.updateContent("삭제된 댓글입니다");
        comment.softDelete();
        // 비동기 댓글수 -1
        postStatAsyncService.decrementCommentCount(postId);
        return ApiResponse.deleted(null);
    }
}
