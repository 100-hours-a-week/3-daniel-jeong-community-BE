package com.kakaotechbootcamp.community.dto.image;

// Presigned URL 응답 DTO
public record PresignedUrlResponseDto(
        String presignedUrl,
        String objectKey,
        String publicUrl,
        int expiresIn
) {
    public static PresignedUrlResponseDto of(String presignedUrl, String objectKey, String publicUrl, int expiresIn) {
        return new PresignedUrlResponseDto(presignedUrl, objectKey, publicUrl, expiresIn);
    }
}
