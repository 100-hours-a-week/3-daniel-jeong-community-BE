package com.kakaotechbootcamp.community.dto.image;

/**
 * 이미지 업로드 응답 DTO
 * - 의도: 업로드 결과 및 접근 가능한 URL 반환
 * - 사용처: ImageUploadController 응답, UserReferenceDto(프로필 응답)
 * - url: S3 Public URL (https://bucket.s3.region.amazonaws.com/objectKey)
 */
public record ImageUploadResponseDto(
        /**
         * S3 objectKey 경로
         * - 형식: {imageType}/{resourceId}/{path}/{filename}
         */
        String objectKey,
        
        /**
         * 접근 가능한 이미지 URL (nullable)
         * - 업로드 API: 현재는 /files/**
         * - 프로필 응답: null 허용 (objectKey만 사용)
         */
        String url
) {
    /**
     * objectKey와 url을 모두 받아 생성
     * - 의도: 이미지 업로드 API 응답 (url 포함)
     */
    public static ImageUploadResponseDto of(String objectKey, String url) {
        return new ImageUploadResponseDto(objectKey, url);
    }

    /**
     * objectKey만으로 생성 (url은 null)
     * - 의도: 프로필 이미지 응답 등 url이 필요 없는 경우
     * - 사용: UserReferenceDto.from()에서 프로필 이미지 표현
     */
    public static ImageUploadResponseDto of(String objectKey) {
        return new ImageUploadResponseDto(objectKey, null);
    }
}
