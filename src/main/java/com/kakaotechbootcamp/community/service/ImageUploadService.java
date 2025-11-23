package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ImageType;
import com.kakaotechbootcamp.community.dto.image.ImageUploadResponseDto;
import com.kakaotechbootcamp.community.exception.NotFoundException;
import com.kakaotechbootcamp.community.common.ImageProperties;
import com.kakaotechbootcamp.community.exception.BadRequestException;
import com.kakaotechbootcamp.community.repository.PostRepository;
import com.kakaotechbootcamp.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

/**
 * 이미지 업로드 서비스
 * 프로필: 고정 파일명 (replace), 게시글: 고유 파일명
 */
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ImageProperties imageProperties;
    private final S3Service s3Service;

    // 이미지 업로드: objectKey 생성/검증
    @Transactional(readOnly = true)
    public ImageUploadResponseDto uploadImage(ImageType imageType, String objectKey, String filename, Integer resourceId) {
        validateResourceExists(imageType, resourceId);

        String finalObjectKey = (objectKey != null && !objectKey.isBlank())
                ? validateAndGetObjectKey(imageType, objectKey, resourceId)
                : generateObjectKey(imageType, resourceId, requireNonBlank(filename, "objectKey 또는 filename 중 하나는 필수입니다"));

        return ImageUploadResponseDto.of(finalObjectKey, s3Service.generatePublicUrl(finalObjectKey));
    }

    // objectKey 경로 생성
    public String generateObjectKey(ImageType imageType, Integer resourceId, String filename) {
        String extension = extractAndValidateExtension(filename);
        String pathFormat = getPathFormat(imageType);
        
        if (imageType == ImageType.PROFILE) {
            return String.format(pathFormat, resourceId, "profile." + extension);
        } else if (imageType == ImageType.POST) {
            String safeFilename = sanitizeFilename(filename);
            return String.format(pathFormat, resourceId, safeFilename);
        }
        throw new BadRequestException("지원하지 않는 이미지 타입입니다");
    }

    // objectKey 검증 (외부 서비스용)
    @Transactional(readOnly = true)
    public void validateObjectKey(ImageType imageType, String objectKey, Integer resourceId) {
        if (objectKey != null && !objectKey.isBlank()) {
            validateResourceExists(imageType, resourceId);
            validateObjectKeyPath(imageType, objectKey, resourceId);
        }
    }

    // 리소스 존재 검증
    public void validateResourceExists(ImageType imageType, Integer resourceId) {
        if (resourceId == null) {
            throw new BadRequestException(
                    imageType == ImageType.PROFILE ? "프로필 이미지 검증 시 userId는 필수입니다" : "게시글 이미지 검증 시 postId는 필수입니다");
        }
        
        boolean exists = (imageType == ImageType.PROFILE)
                ? userRepository.existsById(resourceId)
                : postRepository.existsById(resourceId);
        
        if (!exists) {
            throw new NotFoundException(imageType == ImageType.PROFILE ? "사용자를 찾을 수 없습니다" : "게시글을 찾을 수 없습니다");
        }
    }

    // Multipart 파일 업로드
    public ImageUploadResponseDto uploadMultipart(ImageType imageType, Integer resourceId, MultipartFile file) {
        validateResourceExists(imageType, resourceId);

        String originalName = requireNonBlank(file.getOriginalFilename(), "파일명이 필요합니다");
        String extension = extractAndValidateExtension(originalName);

        String objectKey = (imageType == ImageType.PROFILE)
                ? generateObjectKey(imageType, resourceId, originalName)
                : generateObjectKey(imageType, resourceId, buildUniqueFilename(originalName));

        String contentType = getContentType(file, extension);
        
        try {
            s3Service.uploadFile(objectKey, contentType, file.getInputStream(), file.getSize());
        } catch (IOException e) {
            throw new BadRequestException("이미지 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }

        return ImageUploadResponseDto.of(objectKey, s3Service.generatePublicUrl(objectKey));
    }

    // objectKey 검증 및 반환
    private String validateAndGetObjectKey(ImageType imageType, String objectKey, Integer resourceId) {
        validateObjectKeyPath(imageType, objectKey, resourceId);
        extractAndValidateExtension(objectKey);
        return objectKey;
    }

    // objectKey 경로 규칙 검증
    private void validateObjectKeyPath(ImageType imageType, String objectKey, Integer resourceId) {
        String expectedPrefix = String.format(getPathFormat(imageType), resourceId, "");
        if (!objectKey.startsWith(expectedPrefix)) {
            String typeName = imageType == ImageType.PROFILE ? "프로필" : "게시글";
            throw new BadRequestException(String.format("%s 이미지 objectKey는 '%s'로 시작해야 합니다", typeName, expectedPrefix));
        }
    }

    // 경로 포맷 가져오기
    private String getPathFormat(ImageType imageType) {
        String format = (imageType == ImageType.PROFILE)
                ? imageProperties.getProfilePathFormat()
                : imageProperties.getPostPathFormat();
        
        if (format == null || format.isBlank()) {
            return imageType == ImageType.PROFILE ? "user/%d/profile/%s" : "post/%d/images/%s";
        }
        return format;
    }

    // 확장자 추출 및 검증
    private String extractAndValidateExtension(String filename) {
        String ext = extractExtension(filename);
        if (!imageProperties.getAllowedExtensionSet().contains(ext)) {
            throw new BadRequestException("지원하지 않는 이미지 확장자입니다: " + ext + " (" + imageProperties.getAllowedExtensionsAsString() + "만 가능)");
        }
        return ext;
    }

    // 확장자 추출
    private String extractExtension(String nameOrPath) {
        String filename = nameOrPath.substring(nameOrPath.lastIndexOf('/') + 1);
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot + 1 >= filename.length()) {
            throw new BadRequestException("이미지 확장자가 필요합니다 (" + imageProperties.getAllowedExtensionsAsString() + "만 가능)");
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    // 파일명 정리 (경로 분리자, 특수문자 제거)
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[/\\\\]", "").replaceAll("^\\.+", "").trim();
    }

    // 고유 파일명 생성 (basename_uuid.ext)
    private String buildUniqueFilename(String originalName) {
        String name = originalName.substring(originalName.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        String ext = (dot > 0 && dot + 1 < name.length()) ? name.substring(dot + 1) : "";
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return ext.isEmpty() ? (base + "_" + uuid) : (base + "_" + uuid + "." + ext);
    }

    // Content-Type 결정 (확장자로 추론)
    private String getContentType(MultipartFile file, String extension) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    // null/빈 문자열 검증
    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }
}
