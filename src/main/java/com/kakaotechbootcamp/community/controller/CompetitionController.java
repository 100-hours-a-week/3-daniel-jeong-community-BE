package com.kakaotechbootcamp.community.controller;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.dto.competition.CompetitionResponseDto;
import com.kakaotechbootcamp.community.service.CompetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 수영대회(Competition) API 컨트롤러
 */
@RestController
@RequestMapping("/api/competitions")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;

    /**
     * 대회 목록 조회
     * - 파라미터: year(연도), month(월), type(유형: masters, official, regional)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CompetitionResponseDto>> list(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "type", required = false) String type
    ) {
        ApiResponse<CompetitionResponseDto> response = competitionService.list(year, month, type);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
