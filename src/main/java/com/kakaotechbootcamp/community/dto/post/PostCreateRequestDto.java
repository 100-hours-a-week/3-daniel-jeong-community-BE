package com.kakaotechbootcamp.community.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

/**
 * 게시글 생성 요청 DTO
 * - 의도: 작성자/제목/내용/이미지 키 목록을 받아 게시글을 생성
 * - 제약: 제목 최대 26자, 내용 필수
 */
@Getter
public class PostCreateRequestDto {
    @NotNull(message = "작성자 ID는 필수입니다")
    private Integer userId;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 26, message = "제목은 26자 이하여야 합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String content;

    private List<String> imageObjectKeys;
}
