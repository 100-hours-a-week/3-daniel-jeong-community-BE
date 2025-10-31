package com.kakaotechbootcamp.community.config;

import com.kakaotechbootcamp.community.common.ImageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

/**
 * 정적 리소스 매핑 설정
 * - 의도: 로컬 저장소(uploads/)에 저장된 파일을 /files/** 경로로 서빙
 */
@Configuration
@RequiredArgsConstructor
public class StaticResourceConfig implements WebMvcConfigurer {

    private final ImageProperties imageProperties;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String baseDir = imageProperties.getBaseDir();
        if (baseDir == null || baseDir.isBlank()) {
            baseDir = "uploads";
        }
        if (!baseDir.endsWith("/")) {
            baseDir = baseDir + "/";
        }
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + baseDir);
    }
}
