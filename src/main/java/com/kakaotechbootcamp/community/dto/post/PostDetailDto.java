package com.kakaotechbootcamp.community.dto.post;

import com.kakaotechbootcamp.community.dto.comment.CommentResponseDto;
import com.kakaotechbootcamp.community.dto.user.UserReferenceDto;
import com.kakaotechbootcamp.community.entity.Comment;
import com.kakaotechbootcamp.community.entity.Post;
import com.kakaotechbootcamp.community.entity.PostImage;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 게시글 상세 응답 DTO
 * - 의도: 본문/작성자/이미지/통계/댓글을 통합 제공
 */
public record PostDetailDto(
        Integer postId,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UserReferenceDto author,
        List<String> imageObjectKeys,
        PostStatResponseDto stats,
        List<CommentResponseDto> comments,
        boolean isLiked
) {
    public static PostDetailDto from(Post post, List<PostImage> images, PostStatResponseDto stats, List<Comment> comments, boolean isLiked) {
        List<String> keys = images == null ? Collections.emptyList() : images.stream()
                .map(PostImage::getObjectKey)
                .collect(Collectors.toList());
        List<CommentResponseDto> commentDtos = comments == null ? Collections.emptyList() : comments.stream()
                .map(CommentResponseDto::from)
                .toList();
        return new PostDetailDto(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                UserReferenceDto.from(post.getUser()),
                keys,
                stats,
                commentDtos,
                isLiked
        );
    }
}
