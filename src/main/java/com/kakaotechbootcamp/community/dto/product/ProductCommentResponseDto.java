package com.kakaotechbootcamp.community.dto.product;

import com.kakaotechbootcamp.community.dto.user.UserReferenceDto;
import com.kakaotechbootcamp.community.entity.ProductComment;

import java.time.LocalDateTime;

/**
 * 중고거래 상품 댓글 응답 DTO
 */
public record ProductCommentResponseDto(
        Integer commentId,
        Integer parentId,
        String content,
        Integer depth,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UserReferenceDto author
) {

    public static ProductCommentResponseDto from(ProductComment comment) {
        return new ProductCommentResponseDto(
                comment.getId(),
                comment.getParentId(),
                comment.getContent(),
                comment.getDepth(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                UserReferenceDto.from(comment.getUser())
        );
    }
}
