package com.kakaotechbootcamp.community.dto.competition;

import com.kakaotechbootcamp.community.entity.CompetitionStatus;
import com.kakaotechbootcamp.community.entity.CompetitionType;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 수영대회 목록 항목 DTO
 */
public record CompetitionListItemDto(
        Integer id,
        CompetitionType type,
        String name,
        LocalDate eventDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        CompetitionStatus status
) {
}
