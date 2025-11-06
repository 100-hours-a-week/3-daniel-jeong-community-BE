package com.kakaotechbootcamp.community.controller;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.service.PostLikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 게시글 좋아요 API 컨트롤러
 * - 의도: 게시글에 대한 좋아요 생성/삭제(멱등) 처리
 * - 경로: /posts/{postId}/likes
 */
@RestController
@RequestMapping("/posts/{postId}/likes")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService likeService;

    /**
     * 좋아요 등록(멱등)
     * - 의도: 이미 좋아요인 경우에도 오류 없이 현재 likeCount 반환
     * - 요청 바디: { "userId": number }
     * - 응답: ApiResponse.created({ likeCount, isLiked })
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveLike(
            @PathVariable Integer postId,
            @RequestBody Map<String, Integer> body
    ) {
        Map<String, Object> result = likeService.saveLike(body.get("userId"), postId);
        return ResponseEntity.status(201).body(ApiResponse.created(result));
    }

    /**
     * 좋아요 취소(멱등)
     * - 의도: 아직 좋아요가 아니라면 오류 없이 현재 likeCount 반환
     * - 요청 바디: { "userId": number }
     * - 응답: ApiResponse.deleted({ likeCount, isLiked })
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeLike(
            @PathVariable Integer postId,
            @RequestBody Map<String, Integer> body
    ) {
        Map<String, Object> result = likeService.removeLike(body.get("userId"), postId);
        return ResponseEntity.ok(ApiResponse.deleted(result));
    }
}
