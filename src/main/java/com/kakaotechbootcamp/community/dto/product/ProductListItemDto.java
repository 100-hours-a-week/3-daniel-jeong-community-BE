package com.kakaotechbootcamp.community.dto.product;

import com.kakaotechbootcamp.community.dto.user.UserReferenceDto;
import com.kakaotechbootcamp.community.entity.Product;
import com.kakaotechbootcamp.community.entity.ProductStatus;

import java.time.LocalDateTime;

/**
 * 중고거래 상품 목록 항목 DTO
 */
public record ProductListItemDto(
        Integer productId,
        String title,
        Integer price,
        String category,
        String location,
        ProductStatus status,
        String thumbnailKey,
        Integer viewCount,
        LocalDateTime createdAt,
        UserReferenceDto seller
) {
    public static ProductListItemDto from(Product product, String thumbnailKey) {
        return new ProductListItemDto(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getCategory(),
                product.getLocation(),
                product.getStatus(),
                thumbnailKey,
                product.getViewCount(),
                product.getCreatedAt(),
                UserReferenceDto.from(product.getUser())
        );
    }
}
