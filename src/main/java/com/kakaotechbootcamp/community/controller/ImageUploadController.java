package com.kakaotechbootcamp.community.controller;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.common.ImageProperties;
import com.kakaotechbootcamp.community.config.S3Properties;
import com.kakaotechbootcamp.community.dto.image.PresignedUrlRequestDto;
import com.kakaotechbootcamp.community.dto.image.PresignedUrlResponseDto;
import com.kakaotechbootcamp.community.service.ImageUploadService;
import com.kakaotechbootcamp.community.service.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.kakaotechbootcamp.community.exception.BadRequestException;

import java.util.Map;

/**
 * 이미지 업로드 API 컨트롤러
 * - 역할: 전송 레벨 검증(빈 파일, 최대 크기) + 서비스 위임
 * - 검증 책임 배분:
 *   - Controller: 빈 파일, 크기 제한(max-size-bytes), 확장자 화이트리스트
 *   - Service: 리소스 존재, 경로 규칙(ImageType prefix), 저장 및 URL 생성
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private final ImageUploadService imageUploadService;
    private final ImageProperties imageProperties;
    private final S3Service s3Service;
    private final S3Properties s3Properties;

    /**
     * Presigned URL 생성 API
     * - 의도: Frontend에서 S3에 직접 업로드하기 위한 Presigned URL 발급
     * - 요청: { imageType, resourceId, filename, contentType }
     * - 응답: { presignedUrl, objectKey, expiresIn }
     */
    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponseDto>> generatePresignedUrl(
            @Valid @RequestBody PresignedUrlRequestDto request
    ) {
        // 파일명 검증
        String filename = request.filename();
        if (filename == null || filename.isBlank()) {
            throw new BadRequestException("파일명이 필요합니다");
        }
        
        // 확장자 검증
        String extension = imageProperties.extractExtensionFromContentType(request.contentType());
        if (extension == null || !imageProperties.getAllowedExtensionSet().contains(extension)) {
            throw new BadRequestException("지원하지 않는 이미지 형식입니다. (" + imageProperties.getAllowedExtensionsAsString() + "만 가능)");
        }
        
        // 리소스 존재 검증
        imageUploadService.validateResourceExists(request.imageType(), request.resourceId());
        
        // objectKey 생성
        String objectKey = imageUploadService.generateObjectKey(
                request.imageType(),
                request.resourceId(),
                filename
        );
        
        // Presigned URL 생성
        String presignedUrl = s3Service.generatePresignedUrl(objectKey, request.contentType());
        
        // Public URL 생성
        String publicUrl = s3Service.generatePublicUrl(objectKey);
        
        PresignedUrlResponseDto response = PresignedUrlResponseDto.of(
                presignedUrl,
                objectKey,
                publicUrl,
                s3Properties.getPresignedUrlExpirationMinutes() * 60
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * objectKey로 Public URL 조회
     * - 의도: objectKey만 있는 경우 URL 생성
     */
    @GetMapping("/public-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getImageUrl(@RequestParam String objectKey) {
        String publicUrl = s3Service.generatePublicUrl(objectKey);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", publicUrl)));
    }
}
