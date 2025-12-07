package com.kakaotechbootcamp.community.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

/**
 * 게시글 수정 요청 DTO
 * - 제목/내용: 둘 다 필수(각 1자 이상)
 * - 이미지: 선택 (null=미변경, []=전부 제거, [k1,k2]=해당 순서로 교체)
 */
@Getter
public class PostUpdateRequestDto {
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 26, message = "제목은 26자 이하여야 합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String content;
    private List<String> imageObjectKeys;
}
