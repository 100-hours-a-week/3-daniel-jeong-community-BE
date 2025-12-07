package com.kakaotechbootcamp.community.dto.product;

import java.util.List;

/**
 * 중고거래 상품 목록 응답 DTO (커서 기반 페이지네이션)
 */
public record ProductResponseDto(
        List<ProductListItemDto> items,
        Integer nextCursor,
        boolean hasNext
) {
    public static ProductResponseDto of(List<ProductListItemDto> items, Integer nextCursor, boolean hasNext) {
        return new ProductResponseDto(items, nextCursor, hasNext);
    }
}
