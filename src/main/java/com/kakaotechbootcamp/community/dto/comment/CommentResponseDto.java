package com.kakaotechbootcamp.community.dto.comment;

import com.kakaotechbootcamp.community.dto.user.UserReferenceDto;
import com.kakaotechbootcamp.community.entity.Comment;

import java.time.LocalDateTime;

public record CommentResponseDto(
        Integer commentId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UserReferenceDto author
) {
    public static CommentResponseDto from(Comment c) {
        return new CommentResponseDto(
                c.getId(),
                c.getContent(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                UserReferenceDto.from(c.getUser())
        );
    }
}
