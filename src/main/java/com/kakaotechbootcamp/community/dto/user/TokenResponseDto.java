package com.kakaotechbootcamp.community.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 토큰 응답 DTO
 */
@Getter
@AllArgsConstructor
public class TokenResponseDto {
    private final String accessToken;
    private final String refreshToken;
}

