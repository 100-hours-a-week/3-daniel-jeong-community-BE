package com.kakaotechbootcamp.community.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 세션 인증 필터
 * - 의도: 요청마다 세션 유효성 검증
 * - 인증이 필요한 경로에 대해 세션 확인 후 401 응답 반환
 */
@Component
@RequiredArgsConstructor
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final SessionService sessionService;
    private final ObjectMapper objectMapper;

    /**
     * 인증 불필요한 경로 목록
     * - 회원가입 전 중복 체크 API
     */
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/users/check-email",
            "/users/check-nickname"
    );

    /**
     * 필터 제외 경로 설정
     * - CORS preflight 요청(OPTIONS)은 필터 제외
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return "OPTIONS".equals(request.getMethod());
    }

    /**
     * 필터 실행 로직
     * - 인증 필요한 경로는 세션 검증 후 통과 또는 401 응답
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        // 공개 경로 체크: 인증 불필요한 경로는 통과
        if (isPublicPath(requestPath, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 세션 확인
        HttpSession session = request.getSession(false);
        
        // 세션이 없거나 인증되지 않은 경우 401 응답
        if (!sessionService.isAuthenticated(session)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            ApiResponse<String> errorResponse = ApiResponse.unauthorized("로그인이 필요합니다");
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            response.getWriter().flush();
            return;
        }

        // 세션이 유효한 경우 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * 공개 경로인지 확인
     * - 반환: 공개 경로면 true, 인증 필요 경로면 false
     */
    private boolean isPublicPath(String path, String method) {
        // 정적 리소스 경로는 제외
        if (path.startsWith("/files/") || path.equals("/files")) {
            return true;
        }

        // 정확히 일치하는 공개 경로 (중복 체크)
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }

        // POST /users: 회원가입
        if ("POST".equals(method) && path.equals("/users")) {
            return true;
        }

        // GET /users/{id}: 회원 정보 조회
        if ("GET".equals(method) && path.matches("/users/\\d+")) {
            return true;
        }

        // POST /auth: 로그인
        if ("POST".equals(method) && path.equals("/auth")) {
            return true;
        }

        // DELETE /auth: 로그아웃 (만료된 토큰이어도 로그아웃은 가능할 수 있도록 설정)
        if ("DELETE".equals(method) && path.equals("/auth")) {
            return true;
        }

        // 나머지 경로는 인증 필요
        return false;
    }
}

