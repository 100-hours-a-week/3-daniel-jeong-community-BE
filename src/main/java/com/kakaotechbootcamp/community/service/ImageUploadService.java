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
 * 이미지 업로드 공통 서비스
 * - 의도: 프로필/게시글 이미지 타입별 처리 및 검증 (경로 생성/검증)
 */
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ImageProperties imageProperties;
    private final S3Service s3Service;

    /**
     * 이미지 업로드 검증 및 경로 생성
     * - 의도: 타입별 리소스 존재 확인 후 objectKey 생성/검증
     */
    @Transactional(readOnly = true)
    public ImageUploadResponseDto uploadImage(ImageType imageType, String objectKey, String filename, Integer resourceId) {
        // 리소스 존재 검증
        validateResourceExists(imageType, resourceId);

        // objectKey 생성 또는 검증
        String finalObjectKey;
        if (objectKey != null && !objectKey.isBlank()) {
            // objectKey가 제공된 경우: 경로/확장자 규칙 검증
            validateObjectKeyPath(imageType, objectKey, resourceId);
            validateExtension(objectKey);
            finalObjectKey = objectKey;
        } else {
            // objectKey가 없는 경우: filename으로 경로 자동 생성
            if (filename == null || filename.isBlank()) {
                throw new BadRequestException("objectKey 또는 filename 중 하나는 필수입니다");
            }
            // 확장자 검증
            validateExtension(filename);
            finalObjectKey = generateObjectKey(imageType, resourceId, filename);
        }

        // S3 Public URL 생성
        String url = generateImageUrl(finalObjectKey);
        
        return ImageUploadResponseDto.of(finalObjectKey, url);
    }

    /**
     * 타입별 objectKey 경로 생성
     * - 의도: filename과 resourceId로 규칙에 맞는 경로 자동 생성
     */
    public String generateObjectKey(ImageType imageType, Integer resourceId, String filename) {
        String safeFilename = sanitizeFilename(filename);
        if (imageType == ImageType.PROFILE) {
            String format = imageProperties.getProfilePathFormat();
            if (format == null || format.isBlank()) {
                format = "user/%d/profile/%s"; // fallback
            }
            return String.format(format, resourceId, safeFilename);
        } else if (imageType == ImageType.POST) {
            String format = imageProperties.getPostPathFormat();
            if (format == null || format.isBlank()) {
                format = "post/%d/images/%s"; // fallback
            }
            return String.format(format, resourceId, safeFilename);
        }
        throw new BadRequestException("지원하지 않는 이미지 타입입니다");
    }

    /**
     * objectKey 경로 규칙 검증
     * - 의도: 제공된 objectKey가 타입별 경로 규칙을 따르는지 검증
     */
    private void validateObjectKeyPath(ImageType imageType, String objectKey, Integer resourceId) {
        String expectedPrefix;
        if (imageType == ImageType.PROFILE) {
            expectedPrefix = String.format("user/%d/profile/", resourceId);
            if (!objectKey.startsWith(expectedPrefix)) {
                throw new BadRequestException(
                        String.format("프로필 이미지 objectKey는 '%s'로 시작해야 합니다", expectedPrefix)
                );
            }
        } else if (imageType == ImageType.POST) {
            expectedPrefix = String.format("post/%d/images/", resourceId);
            if (!objectKey.startsWith(expectedPrefix)) {
                throw new BadRequestException(
                        String.format("게시글 이미지 objectKey는 '%s'로 시작해야 합니다", expectedPrefix)
                );
            }
        } else {
            throw new BadRequestException("지원하지 않는 이미지 타입입니다");
        }
    }

    /**
     * filename 정리 (경로 분리자, 특수문자 제거)
     * - 의도: 보안을 위해 경로 분리자 및 위험한 문자 제거
     */
    private String sanitizeFilename(String filename) {
        // 경로 분리자 제거 및 기본 정리
        return filename.replaceAll("[/\\\\]", "")
                .replaceAll("^\\.+", "")  // 선행 점 제거
                .trim();
    }

    /**
     * objectKey 경로 규칙 검증 (외부 서비스용)
     * - 의도: PostService, UserService에서 받은 objectKey 검증
     */
    @Transactional(readOnly = true)
    public void validateObjectKey(ImageType imageType, String objectKey, Integer resourceId) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        
        // 리소스 존재 검증
        validateResourceExists(imageType, resourceId);
        
        // 경로 규칙 검증
        validateObjectKeyPath(imageType, objectKey, resourceId);
    }

    /**
     * 확장자 검증
     * - 의도: 허용된 이미지 확장자만 통과
     */
    private void validateExtension(String nameOrPath) {
        if (nameOrPath == null || nameOrPath.isBlank()) {
            throw new BadRequestException("파일명이 필요합니다");
        }
        // 마지막 세그먼트 추출
        String filename = nameOrPath;
        int slash = filename.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < filename.length()) {
            filename = filename.substring(slash + 1);
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot + 1 >= filename.length()) {
            throw new BadRequestException("이미지 확장자가 필요합니다 (.jpeg/.jpg/.png/.gif/.webp)");
        }
        String ext = filename.substring(dot + 1).toLowerCase();
        if (!imageProperties.getAllowedExtensionSet().contains(ext)) {
            throw new BadRequestException("지원하지 않는 이미지 확장자입니다: " + ext);
        }
    }

    /**
     * 리소스 존재 검증 (공통)
     * - 의도: 타입별 리소스(userId/postId) 존재 확인
     */
    public void validateResourceExists(ImageType imageType, Integer resourceId) {
        if (imageType == ImageType.PROFILE) {
            if (resourceId == null) {
                throw new BadRequestException("프로필 이미지 검증 시 userId는 필수입니다");
            }
            if (!userRepository.existsById(resourceId)) {
                throw new NotFoundException("사용자를 찾을 수 없습니다");
            }
        } else if (imageType == ImageType.POST) {
            if (resourceId == null) {
                throw new BadRequestException("게시글 이미지 검증 시 postId는 필수입니다");
            }
            if (!postRepository.existsById(resourceId)) {
                throw new NotFoundException("게시글을 찾을 수 없습니다");
            }
        }
    }

    /**
     * 이미지 URL 생성
     * - 의도: objectKey를 기반으로 접근 가능한 S3 Public URL 생성
     */
    private String generateImageUrl(String objectKey) {
        return s3Service.generatePublicUrl(objectKey);
    }

    /**
     * Multipart 업로드 처리
     * - 의도: 서버가 파일을 직접 받아 정책 검증 후 objectKey 생성
     */
    public ImageUploadResponseDto uploadMultipart(ImageType imageType, Integer resourceId, MultipartFile file) {
        // 리소스 존재 검증
        validateResourceExists(imageType, resourceId);

        // 컨트롤러에서 파일/크기/확장자 1차 검증 수행

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BadRequestException("파일명이 필요합니다");
        }
        // 확장자 검증
        validateExtension(originalName);

        // 고유 파일명 생성(충돌 방지): {basename}_{uuid}.{ext}
        String uniqueName = buildUniqueFilename(originalName);

        // objectKey 생성
        String objectKey = generateObjectKey(imageType, resourceId, uniqueName);

        // S3에 직접 업로드
        try {
            String contentType = file.getContentType();
            if (contentType == null || contentType.isBlank()) {
                // Content-Type이 없으면 확장자로 추론
                String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
                contentType = switch (ext) {
                    case "jpg", "jpeg" -> "image/jpeg";
                    case "png" -> "image/png";
                    case "gif" -> "image/gif";
                    case "webp" -> "image/webp";
                    default -> "application/octet-stream";
                };
            }
            s3Service.uploadFile(objectKey, contentType, file.getInputStream(), file.getSize());
        } catch (IOException e) {
            throw new BadRequestException("이미지 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }

        // S3 Public URL 생성
        String url = generateImageUrl(objectKey);
        return ImageUploadResponseDto.of(objectKey, url);
    }

    /**
     * 고유 파일명 생성: basename_uuid.ext
     */
    private String buildUniqueFilename(String originalName) {
        String name = originalName;
        int slash = name.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < name.length()) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        String ext = (dot > 0 && dot + 1 < name.length()) ? name.substring(dot + 1) : "";
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return ext.isEmpty() ? (base + "_" + uuid) : (base + "_" + uuid + "." + ext);
    }

}
