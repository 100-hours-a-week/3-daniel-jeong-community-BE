package com.kakaotechbootcamp.community.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.common.Constants;
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
    private final ObjectMapper objectMapper;

    // 필터 제외 경로 설정
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // CORS preflight 요청(OPTIONS)은 필터 제외
        if (Constants.HttpMethod.OPTIONS.equals(method)) {
            return true;
        }
        
        // 정적 리소스는 필터 제외 (확장자 기반)
        if (isStaticResource(path)) {
            return true;
        }
        
        // 제외 경로 목록에 포함된 경로는 필터 제외
        if (Constants.ExcludePath.FILTER_EXCLUDED.stream().anyMatch(path::startsWith)) {
            return true;
        }
        
        // /auth 경로는 POST(로그인), DELETE(로그아웃) 제외
        if (path.equals(Constants.ApiPath.AUTH) && (Constants.HttpMethod.POST.equals(method) || Constants.HttpMethod.DELETE.equals(method))) {
            return true;
        }
        
        // /users 경로는 POST만 제외 (회원가입)
        if (path.equals(Constants.ApiPath.USERS) && Constants.HttpMethod.POST.equals(method)) {
            return true;
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

        String path = request.getRequestURI();
        boolean isPublicGet = path.startsWith(Constants.ApiPath.POSTS) && Constants.HttpMethod.GET.equals(request.getMethod());
        Optional<String> token = extractToken(request);

        // 공개 GET 요청: 토큰이 있으면 userId 설정, 없어도 진행
        if (isPublicGet) {
            token.ifPresent(t -> validateAndSetAttributes(t, request));
            chain.doFilter(request, response);
            return;
        }

        // 인증 필요: 토큰 없거나 유효하지 않으면 에러
        if (token.isEmpty() || !validateAndSetAttributes(token.get(), request)) {
            String uri = request.getRequestURI();
            if (Constants.PagePath.ROOT.equals(uri) || Constants.PagePath.INDEX.equals(uri)) {
                response.sendRedirect(Constants.PagePath.LOGIN);
            } else {
                ApiResponse<Void> apiResponse = ApiResponse.unauthorized(null);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(Constants.ContentType.APPLICATION_JSON);
                response.setCharacterEncoding(Constants.ContentType.UTF8);
                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                response.getWriter().flush();
            }
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 정적 리소스 여부 확인
     * - 경로 기반: /files/, /webjars/
     * - resources/static 하위 리소스: 실제 파일 존재 여부로 확인
     */
    private boolean isStaticResource(String path) {
        // 경로 기반 체크
        if (path.startsWith(Constants.StaticPath.FILES) || 
            path.startsWith(Constants.StaticPath.WEBJARS)) {
            return true;
        }
        
        // Spring Boot 기본 정적 리소스 경로 체크
        // resources/static 하위 파일은 루트 경로로 서빙됨
        // 확장자가 있고, API 경로가 아니면 정적 리소스로 간주
        if (path.contains(".") && 
            !path.startsWith(Constants.ApiPath.AUTH) &&
            !path.startsWith(Constants.ApiPath.USERS) &&
            !path.startsWith(Constants.ApiPath.POSTS)) {
            return true;
        }
        
        return false;
    }

    /**
     * 토큰 추출 (헤더 우선, 쿠키 다음)
     * - Authorization 헤더의 Bearer 토큰을 먼저 확인
     * - 없으면 쿠키의 accessToken 확인
     */
    private Optional<String> extractToken(HttpServletRequest request) {
        // 헤더에서 추출 시도
        String authHeader = request.getHeader(Constants.Header.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(Constants.Header.BEARER_PREFIX)) {
            return Optional.of(authHeader.substring(Constants.Header.BEARER_PREFIX_LENGTH));
        }
        
        // 쿠키에서 추출 시도
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(c -> Constants.Cookie.ACCESS_TOKEN.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst();
        }
        
        return Optional.empty();
    }

    /**
     * 토큰 검증 및 요청 속성 설정
     * - JWT 토큰을 파싱하여 userId와 role을 request attribute로 설정
     * - 검증 실패 시 false 반환
     */
    private boolean validateAndSetAttributes(String token, HttpServletRequest request) {
        try {
            Claims body = jwtProvider.parse(token).getBody();
            request.setAttribute(Constants.RequestAttr.USER_ID, Integer.valueOf(body.getSubject()));
            request.setAttribute(Constants.RequestAttr.ROLE, body.get(JwtProvider.CLAIM_ROLE));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
