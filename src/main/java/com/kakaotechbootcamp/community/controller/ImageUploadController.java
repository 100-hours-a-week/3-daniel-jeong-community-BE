package com.kakaotechbootcamp.community.controller;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.common.ImageProperties;
import com.kakaotechbootcamp.community.config.S3Properties;
import com.kakaotechbootcamp.community.dto.image.ImageUploadRequestDto;
import com.kakaotechbootcamp.community.dto.image.ImageUploadResponseDto;
import com.kakaotechbootcamp.community.dto.image.PresignedUrlRequestDto;
import com.kakaotechbootcamp.community.dto.image.PresignedUrlResponseDto;
import com.kakaotechbootcamp.community.common.ImageType;
import com.kakaotechbootcamp.community.service.ImageUploadService;
import com.kakaotechbootcamp.community.service.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.kakaotechbootcamp.community.exception.BadRequestException;

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
     * 이미지 업로드 (Multipart)
     * - 의도: 서버가 파일을 직접 받아 정책 검증 후 저장 및 응답
     * - 요청: form-data(imageType, resourceId, file)
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ImageUploadResponseDto>> uploadImage(
            @RequestParam("imageType") ImageType imageType,
            @RequestParam("resourceId") Integer resourceId,
            @RequestPart("file") MultipartFile file
    ) {
        // 간단 검증 (빠른 실패)
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("업로드 파일이 비어있습니다");
        }
        if (file.getSize() > imageProperties.getMaxSizeBytes()) {
            throw new BadRequestException("이미지 최대 크기" + imageProperties.getMaxSizeBytes() + "바이트를 초과했습니다");
        }
        if (!imageProperties.isAllowedContentType(file.getContentType())) {
            throw new BadRequestException("지원하지 않는 이미지 형식입니다. (" + imageProperties.getAllowedExtensionsAsString() + "만 가능)");
        }

        ImageUploadResponseDto response = imageUploadService.uploadMultipart(imageType, resourceId, file);
        return ResponseEntity.status(201).body(ApiResponse.created(response));
    }

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
        
        PresignedUrlResponseDto response = PresignedUrlResponseDto.of(
                presignedUrl,
                objectKey,
                s3Properties.getPresignedUrlExpirationMinutes() * 60
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 이미지 업로드 (JSON - objectKey/filename)
     * - 의도: 외부 스토리지(S3 등)에 업로드 후 경로 정보만 전달하는 케이스
     */
    @PostMapping("/upload-object-key")
    public ResponseEntity<ApiResponse<ImageUploadResponseDto>> uploadImageJson(
            @Valid @RequestBody ImageUploadRequestDto request
    ) {
        // 간단 검증 (빠른 실패)
        if ((request.objectKey() == null || request.objectKey().isBlank()) &&
            (request.filename() == null || request.filename().isBlank())) {
            throw new BadRequestException("objectKey 또는 filename 중 하나는 필수입니다");
        }
        ImageUploadResponseDto response = imageUploadService.uploadImage(
                request.imageType(),
                request.objectKey(),
                request.filename(),
                request.resourceId()
        );
        return ResponseEntity.status(201).body(ApiResponse.created(response));
    }
}
