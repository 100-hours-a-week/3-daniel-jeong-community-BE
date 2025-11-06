package com.kakaotechbootcamp.community.controller;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.dto.user.*;
import com.kakaotechbootcamp.community.service.EmailService;
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
    private final EmailService emailService;

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
     * - 의도: 쿠키를 즉시 만료시키고 DB의 refresh token도 무효화
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        userService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.modified(null));
    }

    /**
     * 비밀번호 재설정 인증번호 발송
     * - 의도: 이메일로 인증번호 생성 및 발송
     */
    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse<Integer>> sendPasswordResetCode(
            @Valid @RequestBody PasswordResetRequestDto request
    ) {
        String email = request.getEmail().trim().toLowerCase();
        Integer userId = emailService.sendPasswordResetCode(email);
        return ResponseEntity.ok(ApiResponse.modified(userId));
    }

    /**
     * 비밀번호 재설정 인증번호 검증
     */
    @PostMapping("/password-reset/{id}/verify")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> verifyPasswordResetCode(
            @PathVariable Integer id,
            @RequestBody java.util.Map<String, String> body
    ) {
        ApiResponse<java.util.Map<String, Object>> response = emailService.verifyPasswordResetCode(
                id,
                body.get("verificationCode")
        );
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 비밀번호 재설정
     * - 의도: 인증번호 검증 완료 후 비밀번호 재설정
     */
    @PatchMapping("/password-reset/{id}")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Integer id,
            @Valid @RequestBody PasswordUpdateRequestDto request
    ) {
        ApiResponse<Void> response = emailService.resetPassword(
                id,
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
