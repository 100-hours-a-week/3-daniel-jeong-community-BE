package com.kakaotechbootcamp.community.dto.post;

import com.kakaotechbootcamp.community.entity.PostStat;

/**
 * 게시글 통계 DTO
 * - 의도: 좋아요/댓글/조회수의 집계 정보를 표현하는 응답 DTO
 */
public record PostStatResponseDto(
        int likeCount,
        int commentCount,
        int viewCount
) {
    public static PostStatResponseDto from(PostStat stat) {
        return new PostStatResponseDto(
                stat == null ? 0 : stat.getLikeCount(),
                stat == null ? 0 : stat.getCommentCount(),
                stat == null ? 0 : stat.getViewCount()
        );
    }
}
