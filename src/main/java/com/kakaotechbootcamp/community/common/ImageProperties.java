package com.kakaotechbootcamp.community.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 이미지 정책 설정
 * - 의도: 확장자/최대 크기/최대 개수 중앙관리
 * - 설정 소스: application.yml 의 image.*
 */
@Component
@ConfigurationProperties(prefix = "image")
@Getter
@Setter
public class ImageProperties {

    private long maxSizeBytes;        // 최대 파일 크기(바이트)
    private int maxPerPost;           // 게시글당 이미지 최대 개수
    private List<String> allowedExtensions; // 허용 확장자 목록

    // 경로 포맷 설정 (String#format 사용)
    // 예: user/%d/profile/%s, post/%d/images/%s
    private String profilePathFormat; // PROFILE 경로 포맷
    private String postPathFormat;    // POST 경로 포맷

    public Set<String> getAllowedExtensionSet() {
        Set<String> set = new HashSet<>();
        if (allowedExtensions != null) {
            for (String ext : allowedExtensions) {
                if (ext != null) set.add(ext.toLowerCase());
            }
        }
        return set;
    }
    
    /**
     * Content-Type에서 확장자 추출 (예: image/png -> png)
     */
    public String extractExtensionFromContentType(String contentType) {
        if (contentType == null || !contentType.startsWith("image/")) {
            return null;
        }
        return contentType.substring("image/".length());
    }
    
    /**
     * Content-Type이 허용된 확장자인지 확인
     */
    public boolean isAllowedContentType(String contentType) {
        String extension = extractExtensionFromContentType(contentType);
        return extension != null && getAllowedExtensionSet().contains(extension);
    }
}
