package com.kakaotechbootcamp.community.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CommentRequestDto {
    @NotNull(message = "게시글 ID는 필수입니다")
    private Integer postId;

    private Integer parentId;

    @NotBlank(message = "댓글 내용을 입력해주세요")
    @Size(max = 500, message = "댓글 내용은 500자 이하여야 합니다")
    private String content;
}
