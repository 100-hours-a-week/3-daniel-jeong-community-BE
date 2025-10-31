package com.kakaotechbootcamp.community.dto.post;

import com.kakaotechbootcamp.community.dto.user.UserReferenceDto;
import com.kakaotechbootcamp.community.entity.Post;
import com.kakaotechbootcamp.community.entity.PostStat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 게시글 목록 아이템 DTO
 * - 의도: 목록 행에 필요한 최소 정보(제목/작성자/통계/생성일)
 */
public record PostListItemDto(
        Integer postId,
        String title,
        LocalDateTime createdAt,
        UserReferenceDto author,
        PostStatResponseDto stats
) {
    public static List<PostListItemDto> from(List<Post> posts, Map<Integer, PostStat> postIdToStat) {
        return posts.stream()
                .map(p -> new PostListItemDto(
                        p.getId(),
                        p.getTitle(),
                        p.getCreatedAt(),
                        UserReferenceDto.from(p.getUser()),
                        PostStatResponseDto.from(postIdToStat.get(p.getId()))
                ))
                .toList();
    }
}
