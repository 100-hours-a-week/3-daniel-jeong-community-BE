package com.kakaotechbootcamp.community.dto.competition;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kakaotechbootcamp.community.entity.CompetitionType;
import lombok.Data;

/**
 * JSON 파일에서 읽어올 대회 데이터 DTO
 */
@Data
public class CompetitionDataDto {
    private CompetitionType type;
    private String name;
    
    @JsonProperty("eventDate")
    private String eventDateStr;
    
    @JsonProperty("endDate")
    private String endDateStr;
    
    @JsonProperty("startTime")
    private String startTimeStr;
    
    @JsonProperty("endTime")
    private String endTimeStr;
    
    private String location;
}
