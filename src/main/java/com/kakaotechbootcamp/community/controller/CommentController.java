package com.kakaotechbootcamp.community.controller;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.dto.comment.CommentRequestDto;
import com.kakaotechbootcamp.community.dto.comment.CommentResponseDto;
import com.kakaotechbootcamp.community.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 댓글(Comment) API 컨트롤러
 * - 역할: 요청 유효성 처리 및 CommentService 위임
 */
@RestController
@RequestMapping("/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글 목록 조회 (게시글 기준, 생성일 오름차순)
     * - 에러: 게시글 미존재 시 404 반환
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponseDto>>> listByPost(@PathVariable Integer postId) {
        ApiResponse<List<CommentResponseDto>> response = commentService.listByPost(postId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 댓글 생성
     * - 요청: path postId, param userId, body CommentRequestDto(parentId 선택)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponseDto>> create(
            @PathVariable Integer postId,
            @RequestParam("userId") Integer userId,
            @Valid @RequestBody CommentRequestDto request
    ) {
        ApiResponse<CommentResponseDto> response = commentService.create(postId, userId, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 댓글 수정 (내용만)
     * - 요청: body { content }
     */
    @PatchMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponseDto>> update(
            @PathVariable Integer postId,
            @PathVariable Integer commentId,
            @RequestBody Map<String, String> body
    ) {
        String content = body == null ? null : body.get("content");
        ApiResponse<CommentResponseDto> response = commentService.update(commentId, content);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer postId, @PathVariable Integer commentId) {
        ApiResponse<Void> response = commentService.delete(commentId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
