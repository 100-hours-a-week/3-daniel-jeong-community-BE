package com.kakaotechbootcamp.community.dto.product;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.util.List;

/**
 * 중고거래 상품 수정 요청 DTO
 */
@Getter
public class ProductUpdateRequestDto {
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 26, message = "제목은 26자 이하여야 합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String content;

    @NotNull(message = "가격은 필수입니다")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다")
    @Max(value = 10000000, message = "가격은 1,000만원 이하여야 합니다")
    private Integer price;

    @NotBlank(message = "카테고리는 필수입니다")
    @Pattern(regexp = "^(swimsuit|goggles|cap|training|fins|bag)$", 
             message = "유효하지 않은 카테고리입니다")
    private String category;

    @NotBlank(message = "거래 위치는 필수입니다")
    @Size(max = 26, message = "거래 위치는 26자 이하여야 합니다")
    private String location;

    @Pattern(regexp = "^(SELLING|RESERVED|SOLD)$", 
             message = "유효하지 않은 상태입니다")
    private String status;

    private List<String> imageObjectKeys;
}
