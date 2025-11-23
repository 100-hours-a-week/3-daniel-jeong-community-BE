package com.kakaotechbootcamp.community.common;

/**
 * 애플리케이션 전역 상수
 * - 쿠키 이름, Request Attribute, Request 키, HTTP 메서드, 경로 등 공통으로 사용되는 상수 관리
 */
public final class Constants {
    
    // ===================== 그룹 클래스 =====================
    public static final class Cookie {
        public static final String ACCESS_TOKEN = "accessToken";
        public static final String REFRESH_TOKEN = "refreshToken";
        private Cookie() {}
    }
    public static final class RequestAttr {
        public static final String USER_ID = "userId";
        public static final String ROLE = "role";
        private RequestAttr() {}
    }
    public static final class RequestKey {
        public static final String USER_ID = "userId";
        private RequestKey() {}
    }
    public static final class HttpMethod {
        public static final String OPTIONS = "OPTIONS";
        public static final String GET = "GET";
        public static final String POST = "POST";
        public static final String DELETE = "DELETE";
        private HttpMethod() {}
    }
    public static final class ApiPath {
        public static final String AUTH = "/api/auth";
        public static final String USERS = "/api/users";
        public static final String POSTS = "/api/posts";
        public static final String IMAGES = "/api/images";
        private ApiPath() {}
    }
    public static final class PagePath {
        public static final String ROOT = "/";
        public static final String INDEX = "/index";
        public static final String LOGIN = "/login";
        private PagePath() {}
    }
    public static final class StaticPath {
        public static final String WEBJARS = "/webjars/";
        private StaticPath() {}
    }
    public static final class Header {
        public static final String AUTHORIZATION = "Authorization";
        public static final String BEARER_PREFIX = "Bearer ";
        public static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();
        private Header() {}
    }
    public static final class ContentType {
        public static final String APPLICATION_JSON = "application/json";
        public static final String UTF8 = "UTF-8";
        private ContentType() {}
    }
    public static final class ExcludePath {
        public static final String AUTH_REFRESH = "/api/auth/refresh";
        public static final String AUTH_PASSWORD_RESET = "/api/auth/password-reset";
        public static final String USERS_CHECK_EMAIL = "/api/users/check-email";
        public static final String USERS_CHECK_NICKNAME = "/api/users/check-nickname";
        public static final String ERROR = "/error";
        public static final String TERMS = "/terms";
        public static final String PRIVACY = "/privacy";
        
        // 필터 제외 경로 목록
        public static final java.util.List<String> FILTER_EXCLUDED = java.util.List.of(
            AUTH_REFRESH,
            AUTH_PASSWORD_RESET,
            USERS_CHECK_EMAIL,
            USERS_CHECK_NICKNAME,
            ERROR,
            TERMS,
            PRIVACY
        );
        
        private ExcludePath() {}
    }
}
