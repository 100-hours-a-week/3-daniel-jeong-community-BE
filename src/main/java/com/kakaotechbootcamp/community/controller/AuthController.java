package com.kakaotechbootcamp.community.controller;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.dto.user.*;
import com.kakaotechbootcamp.community.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증(Auth) API 컨트롤러
 * - 로그인, 토큰 갱신, 로그아웃
 * @RestController: @Controller + @ResponseBody
 * @RequestMapping: 기본 경로 /auth
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 로그인 (인증 생성)
     * - 의도: 이메일/비밀번호 검증 후 JWT 토큰 발급 및 쿠키 설정
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserLoginResponseDto>> login(
            @Valid @RequestBody UserLoginRequestDto request,
            HttpServletResponse response
    ) {
        ApiResponse<UserLoginResponseDto> apiResponse = userService.login(request, response);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    /**
     * 리프레시 토큰으로 새 액세스 토큰 발급
     * - 의도: 쿠키에서 refresh token을 읽어 새 액세스 토큰만 발급 (refresh token은 유지)
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ApiResponse<TokenResponseDto> apiResponse = userService.refresh(request, response);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    /**
     * 로그아웃 (인증 삭제)
     * - 의도: 쿠키를 즉시 만료시켜 로그아웃 처리
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        userService.logout(response);
        return ResponseEntity.ok(ApiResponse.modified(null));
    }
}
