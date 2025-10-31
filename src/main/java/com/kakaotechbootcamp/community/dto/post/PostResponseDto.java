package com.kakaotechbootcamp.community.dto.post;

import java.util.List;

/**
 * 게시글 목록 응답 DTO
 * - 의도: 커서 기반 페이지네이션 메타와 목록 아이템 반환
 */
public record PostResponseDto(
        List<PostListItemDto> items,
        Integer nextCursor,
        boolean hasNext
) {}
