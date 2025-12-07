package com.kakaotechbootcamp.community.dto.product;

import com.kakaotechbootcamp.community.dto.user.UserReferenceDto;
import com.kakaotechbootcamp.community.entity.Product;
import com.kakaotechbootcamp.community.entity.ProductImage;
import com.kakaotechbootcamp.community.entity.ProductStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 중고거래 상품 상세 응답 DTO
 */
public record ProductDetailDto(
        Integer productId,
        String title,
        String content,
        Integer price,
        String category,
        String location,
        ProductStatus status,
        Integer viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UserReferenceDto seller,
        List<String> imageObjectKeys
) {
    public static ProductDetailDto from(Product product, List<ProductImage> images) {
        List<String> keys = images == null ? Collections.emptyList() : images.stream()
                .map(ProductImage::getObjectKey)
                .toList();
        
        return new ProductDetailDto(
                product.getId(),
                product.getTitle(),
                product.getContent(),
                product.getPrice(),
                product.getCategory(),
                product.getLocation(),
                product.getStatus(),
                product.getViewCount(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                UserReferenceDto.from(product.getUser()),
                keys
        );
    }
}
