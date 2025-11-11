package com.kakaotechbootcamp.community.config;

import com.kakaotechbootcamp.community.filter.SessionAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 웹 필터 설정
 * - 세션 인증 필터 등록 및 순서 설정
 */
@Configuration
@RequiredArgsConstructor
public class WebFilterConfig {

    private final SessionAuthenticationFilter sessionAuthenticationFilter;

    /**
     * 세션 인증 필터 등록
     * - 모든 요청에 대해 세션 인증 검증
     */
    @Bean
    public FilterRegistrationBean<SessionAuthenticationFilter> sessionFilterRegistration() {
        FilterRegistrationBean<SessionAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(sessionAuthenticationFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1); // 필터 실행 순서 (낮을수록 먼저 실행)
        registration.setName("sessionAuthenticationFilter");
        return registration;
    }
}

