package com.kakaotechbootcamp.community.filter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.kakaotechbootcamp.community.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    // 필터 제외 경로 목록
    private static final String[] EXCLUDED_PATHS = {
            "/auth/refresh", "/users/check-email", "/users/check-nickname", "/error"
    };

    // 필터 제외 경로 설정
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // CORS preflight 요청(OPTIONS)은 필터 제외
        if ("OPTIONS".equals(method)) {
            return true;
        }
        
        // 정적 리소스는 무조건 제외
        if (path.startsWith("/files/") || path.equals("/files")) {
            return true;
        }
        
        // 경로 매칭
        if (Arrays.stream(EXCLUDED_PATHS).anyMatch(path::startsWith)) {
            return true;
        }
        
        // /auth 경로는 POST(로그인), DELETE(로그아웃) 제외
        if (path.equals("/auth")) {
            return "POST".equals(method) || "DELETE".equals(method);
        }
        
        // /users 경로는 POST만 제외 (회원가입)
        if (path.equals("/users")) {
            return "POST".equals(method);
        }
        
        // /posts 경로는 GET만 제외 (게시글 목록/상세 조회는 공개)
        if (path.startsWith("/posts")) {
            return "GET".equals(method);
        }
        
        return false;
    }

    // 실제 필터링 로직
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws IOException, ServletException {

        boolean isIndex = isIndexRequest(request);
        Optional<String> token = extractToken(request);

        // 토큰 없음 → 필터 적용된 요청은 인증 필요
        if (token.isEmpty()) {
            if (isIndex) {
                response.sendRedirect("/login");
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
            return;
        }

        // 토큰 검증 및 속성 설정
        if (!validateAndSetAttributes(token.get(), request)) {
            // 토큰이 잘못된 경우 → index면 리다이렉트, 그 외는 401
            if (isIndex) {
                response.sendRedirect("/login");
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
            return;
        }

        chain.doFilter(request, response);
    }

    // index 요청인지 확인
    private boolean isIndexRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "/".equals(uri) || "/index".equals(uri);
    }

    // 토큰 추출 (헤더 우선, 쿠키 다음)
    private Optional<String> extractToken(HttpServletRequest request) {
        return extractTokenFromHeader(request)
                .or(() -> extractTokenFromCookie(request));
    }

    // 헤더에서 토큰 추출
    private Optional<String> extractTokenFromHeader(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Authorization"))
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7));
    }

    // 쿠키에서 토큰 추출
    private Optional<String> extractTokenFromCookie(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())
                .stream()
                .flatMap(Arrays::stream)
                .filter(cookie -> "accessToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    // 토큰 검증 및 요청 속성 설정
    private boolean validateAndSetAttributes(String token, HttpServletRequest request) {
        try {
            var jws = jwtProvider.parse(token);
            Claims body = jws.getBody();
            request.setAttribute("userId", Long.valueOf(body.getSubject()));
            request.setAttribute("role", body.get("role"));
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}