package com.kakaotechbootcamp.community.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 로그인 응답 DTO
 */
@Getter
@AllArgsConstructor
public class UserLoginResponseDto {
    private final String accessToken;
    private final String refreshToken;
    private final UserResponseDto user;
}

