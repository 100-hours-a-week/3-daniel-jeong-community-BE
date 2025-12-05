package com.kakaotechbootcamp.community.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.dto.competition.CompetitionDataDto;
import com.kakaotechbootcamp.community.dto.competition.CompetitionListItemDto;
import com.kakaotechbootcamp.community.dto.competition.CompetitionResponseDto;
import com.kakaotechbootcamp.community.entity.CompetitionStatus;
import com.kakaotechbootcamp.community.entity.CompetitionType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 수영대회(Competition) 도메인 서비스
 * - JSON 파일에서 직접 데이터를 읽어서 반환
 */
@Service
@RequiredArgsConstructor
public class CompetitionService {

    private static final String COMPETITIONS_JSON_PATH = "data/competitions.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ObjectMapper objectMapper;

    /**
     * 대회 목록 조회
     * - JSON 파일에서 직접 읽어서 필터링 및 반환
     * - 연도, 월, 유형 필터링 지원
     */
    public ApiResponse<CompetitionResponseDto> list(Integer year, Integer month, String type) {
        try {
            List<CompetitionDataDto> allCompetitions = loadCompetitionsFromJson();
            LocalDate today = LocalDate.now();

            // DTO를 CompetitionListItemDto로 변환하면서 필터링 및 상태 계산
            List<CompetitionListItemDto> items = allCompetitions.stream()
                    .map(data -> convertToListItemDto(data, today))
                    .filter(item -> {
                        // 연도 필터
                        if (year != null && item.eventDate().getYear() != year) {
                            return false;
                        }
                        // 월 필터
                        if (month != null && item.eventDate().getMonthValue() != month) {
                            return false;
                        }
                        // 유형 필터
                        if (type != null && !type.equals("all")) {
                            try {
                                CompetitionType filterType = CompetitionType.valueOf(type.toUpperCase());
                                if (item.type() != filterType) {
                                    return false;
                                }
                            } catch (IllegalArgumentException e) {
                                // 잘못된 타입은 무시
                            }
                        }
                        return true;
                    })
                    .sorted((a, b) -> {
                        // 날짜 기준 정렬
                        int dateCompare = a.eventDate().compareTo(b.eventDate());
                        if (dateCompare != 0) return dateCompare;
                        // 시간 기준 정렬
                        if (a.startTime() != null && b.startTime() != null) {
                            return a.startTime().compareTo(b.startTime());
                        }
                        return 0;
                    })
                    .toList();

            return ApiResponse.success(CompetitionResponseDto.of(items));
        } catch (IOException e) {
            return ApiResponse.success(CompetitionResponseDto.of(List.of()));
        }
    }

    private List<CompetitionDataDto> loadCompetitionsFromJson() throws IOException {
        ClassPathResource resource = new ClassPathResource(COMPETITIONS_JSON_PATH);
        
        if (!resource.exists()) {
            return List.of();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<CompetitionDataDto>>() {}
            );
        }
    }

    private CompetitionListItemDto convertToListItemDto(CompetitionDataDto data, LocalDate today) {
        // 날짜 파싱
        LocalDate eventDate = LocalDate.parse(data.getEventDateStr(), DATE_FORMATTER);
        LocalDate endDate = data.getEndDateStr() != null 
                ? LocalDate.parse(data.getEndDateStr(), DATE_FORMATTER) 
                : null;
        
        // 시간 파싱
        LocalTime startTime = data.getStartTimeStr() != null 
                ? LocalTime.parse(data.getStartTimeStr(), TIME_FORMATTER) 
                : null;
        LocalTime endTime = data.getEndTimeStr() != null 
                ? LocalTime.parse(data.getEndTimeStr(), TIME_FORMATTER) 
                : null;
        
        // 상태 결정
        CompetitionStatus status = determineStatus(eventDate, endDate, today);
        
        return new CompetitionListItemDto(
                null, // id는 JSON에서 관리하지 않음
                data.getType(),
                data.getName(),
                eventDate,
                endDate,
                startTime,
                endTime,
                data.getLocation(),
                status
        );
    }

    private CompetitionStatus determineStatus(LocalDate eventDate, LocalDate endDate, LocalDate today) {
        if (endDate != null) {
            if (endDate.isBefore(today)) {
                return CompetitionStatus.COMPLETED;
            } else if (eventDate.isBefore(today) || eventDate.isEqual(today)) {
                return CompetitionStatus.ONGOING;
            } else {
                return CompetitionStatus.UPCOMING;
            }
        } else {
            if (eventDate.isBefore(today)) {
                return CompetitionStatus.COMPLETED;
            } else if (eventDate.isEqual(today)) {
                return CompetitionStatus.ONGOING;
            } else {
                return CompetitionStatus.UPCOMING;
            }
        }
    }
}
