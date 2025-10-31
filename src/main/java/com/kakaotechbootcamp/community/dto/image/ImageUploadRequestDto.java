package com.kakaotechbootcamp.community.dto.image;

import com.kakaotechbootcamp.community.common.ImageType;
import jakarta.validation.constraints.NotNull;

/**
 * 이미지 업로드 요청 DTO
 * - 의도: JSON 기반 업로드(objectKey/filename) 시 경로 생성/검증 정보 전달
 * - 기본 업로드는 Multipart이며, 본 DTO는 S3 호환을 위해 유지
 * - 요청 방식:
 *   1) objectKey 제공: 전체 경로 직접 제공(서비스가 prefix/확장자 검증)
 *   2) filename 제공: 파일명만 제공 시 서버가 경로 자동 생성
 * - resourceId: PROFILE=userId, POST=postId
 */
public record ImageUploadRequestDto(
        @NotNull(message = "이미지 타입은 필수입니다")
        ImageType imageType,
        
        /**
         * 전체 objectKey 경로 (optional)
         * - 제공 시: 경로 규칙 검증 후 사용
         * - 미제공 시: filename과 resourceId로 경로 자동 생성
         */
        String objectKey,
        
        /**
         * 파일명 (objectKey가 없을 때만 사용)
         * - objectKey가 null일 때 필수
         */
        String filename,
        
        /**
         * 리소스 ID (필수)
         * - PROFILE: userId
         * - POST: postId
         */
        Integer resourceId
) {
}
