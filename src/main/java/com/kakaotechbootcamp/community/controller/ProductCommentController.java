package com.kakaotechbootcamp.community.controller;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.dto.product.ProductCommentRequestDto;
import com.kakaotechbootcamp.community.dto.product.ProductCommentResponseDto;
import com.kakaotechbootcamp.community.service.ProductCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 중고거래 상품 댓글 API 컨트롤러
 * - 목록 조회, 생성, 수정, 삭제
 */
@RestController
@RequestMapping("/api/products/{productId}/comments")
@RequiredArgsConstructor
public class ProductCommentController {

    private final ProductCommentService productCommentService;

    /**
     * 상품 댓글 목록 조회 (페이지네이션)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductCommentResponseDto>>> listByProduct(
            @PathVariable Integer productId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        ApiResponse<Page<ProductCommentResponseDto>> response = productCommentService.listByProduct(productId, page, size);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 상품 댓글 생성
     * - 요청: path productId, param userId, body ProductCommentRequestDto
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductCommentResponseDto>> create(
            @PathVariable Integer productId,
            @RequestParam("userId") Integer userId,
            @Valid @RequestBody ProductCommentRequestDto request
    ) {
        ApiResponse<ProductCommentResponseDto> response = productCommentService.create(productId, userId, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 상품 댓글 수정
     * - 요청: body { content }
     */
    @PatchMapping("/{commentId}")
    public ResponseEntity<ApiResponse<ProductCommentResponseDto>> update(
            @PathVariable Integer productId,
            @PathVariable Integer commentId,
            @RequestBody Map<String, String> body
    ) {
        String content = body.get("content");
        ApiResponse<ProductCommentResponseDto> response = productCommentService.update(commentId, content);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 상품 댓글 삭제
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Integer productId,
            @PathVariable Integer commentId
    ) {
        ApiResponse<Void> response = productCommentService.delete(commentId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
