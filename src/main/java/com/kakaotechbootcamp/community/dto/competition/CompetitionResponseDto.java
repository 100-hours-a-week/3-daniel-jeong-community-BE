package com.kakaotechbootcamp.community.dto.competition;

import java.util.List;

/**
 * 수영대회 목록 응답 DTO
 */
public record CompetitionResponseDto(
        List<CompetitionListItemDto> items
) {
    public static CompetitionResponseDto of(List<CompetitionListItemDto> items) {
        return new CompetitionResponseDto(items);
    }
}
