package com.kakaotechbootcamp.community.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 중고거래 상품 댓글 생성/수정 요청 DTO
 */
@Getter
public class ProductCommentRequestDto {

    @NotBlank(message = "댓글 내용을 입력해주세요")
    @Size(max = 500, message = "댓글은 500자 이하여야 합니다")
    private String content;

    // 선택: 부모 댓글 ID (대댓글인 경우)
    private Integer parentId;
}
