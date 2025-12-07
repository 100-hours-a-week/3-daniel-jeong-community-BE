package com.kakaotechbootcamp.community.controller;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.dto.product.ProductCreateRequestDto;
import com.kakaotechbootcamp.community.dto.product.ProductDetailDto;
import com.kakaotechbootcamp.community.dto.product.ProductResponseDto;
import com.kakaotechbootcamp.community.dto.product.ProductUpdateRequestDto;
import com.kakaotechbootcamp.community.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 중고거래 상품(Product) API 컨트롤러
 * - 목록 조회(커서 기반), 상세 조회, 생성, 수정, 삭제, 상태 변경
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 목록 조회 (커서 기반 페이지네이션)
     * - 파라미터: cursor, size, category, status, search
     * - 응답: items, nextCursor, hasNext
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ProductResponseDto>> list(
            @RequestParam(value = "cursor", required = false) Integer cursor,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search
    ) {
        ApiResponse<ProductResponseDto> response = productService.list(cursor, size, category, status, search);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 상품 상세 조회
     * - 조회수 +1
     * - 에러: 존재하지 않으면 404(NotFound)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailDto>> getDetail(
            @PathVariable("id") Integer id
    ) {
        ApiResponse<ProductDetailDto> response = productService.getDetail(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 상품 생성
     * - 에러: 판매자 미존재 시 404(NotFound), 유효성 위반 시 400
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDetailDto>> create(
            @Valid @RequestBody ProductCreateRequestDto request
    ) {
        ApiResponse<ProductDetailDto> response = productService.create(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 상품 수정
     * - 에러: 상품 미존재 시 404(NotFound)
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailDto>> update(
            @PathVariable("id") Integer id,
            @Valid @RequestBody ProductUpdateRequestDto request
    ) {
        ApiResponse<ProductDetailDto> response = productService.update(id, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 상품 상태 변경 (판매중, 예약중, 거래완료)
     * - 요청: body { status }
     * - 에러: 상품 미존재 시 404(NotFound)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductDetailDto>> updateStatus(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        ApiResponse<ProductDetailDto> response = productService.updateStatus(id, status);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 상품 삭제 (Soft Delete)
     * - 에러: 상품 미존재 시 404(NotFound)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("id") Integer id
    ) {
        ApiResponse<Void> response = productService.delete(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
