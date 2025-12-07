package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ImageType;
import com.kakaotechbootcamp.community.exception.NotFoundException;
import com.kakaotechbootcamp.community.common.ImageProperties;
import com.kakaotechbootcamp.community.exception.BadRequestException;
import com.kakaotechbootcamp.community.repository.PostRepository;
import com.kakaotechbootcamp.community.repository.ProductRepository;
import com.kakaotechbootcamp.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;

/**
 * 이미지 업로드 서비스
 * 프로필: 고정 파일명 (replace), 게시글: 고유 파일명
 */
@Service
public class ImageUploadService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ProductRepository productRepository;
    private final ImageProperties imageProperties;
    private final Map<ImageType, Function<Integer, Boolean>> resourceExistenceCheckers;

    // ImageType별 메타데이터
    private static final Map<ImageType, ImageTypeMetadata> IMAGE_TYPE_METADATA = Map.of(
        ImageType.PROFILE, new ImageTypeMetadata(
            "프로필", "userId", "사용자를 찾을 수 없습니다", "user/%d/profile/%s"
        ),
        ImageType.POST, new ImageTypeMetadata(
            "게시글", "postId", "게시글을 찾을 수 없습니다", "post/%d/images/%s"
        ),
        ImageType.PRODUCT, new ImageTypeMetadata(
            "상품", "productId", "상품을 찾을 수 없습니다", "product/%d/images/%s"
        )
    );

    // ImageType별 리소스 존재 검증 함수
    public ImageUploadService(
            UserRepository userRepository,
            PostRepository postRepository,
            ProductRepository productRepository,
            ImageProperties imageProperties) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.productRepository = productRepository;
        this.imageProperties = imageProperties;
        
        // 인스턴스 필드가 초기화된 후에 Map 생성
        this.resourceExistenceCheckers = Map.of(
            ImageType.PROFILE, userRepository::existsById,
            ImageType.POST, postRepository::existsById,
            ImageType.PRODUCT, productRepository::existsById
        );
    }

    // objectKey 경로 생성
    public String generateObjectKey(ImageType imageType, Integer resourceId, String filename) {
        String extension = extractAndValidateExtension(filename);
        String pathFormat = getPathFormat(imageType);
        
        String finalFilename = (imageType == ImageType.PROFILE) 
            ? "profile." + extension 
            : sanitizeFilename(filename);
        
        return String.format(pathFormat, resourceId, finalFilename);
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
        ImageTypeMetadata metadata = getMetadata(imageType);
        
        if (resourceId == null) {
            throw new BadRequestException(
                String.format("%s 이미지 검증 시 %s는 필수입니다", metadata.typeName, metadata.resourceIdName)
            );
        }
        
        Function<Integer, Boolean> checker = resourceExistenceCheckers.get(imageType);
        if (checker == null || !checker.apply(resourceId)) {
            throw new NotFoundException(metadata.notFoundMessage);
        }
    }

    // objectKey 경로 규칙 검증
    private void validateObjectKeyPath(ImageType imageType, String objectKey, Integer resourceId) {
        ImageTypeMetadata metadata = getMetadata(imageType);
        String expectedPrefix = String.format(getPathFormat(imageType), resourceId, "");
        
        if (!objectKey.startsWith(expectedPrefix)) {
            throw new BadRequestException(
                String.format("%s 이미지 objectKey는 '%s'로 시작해야 합니다", metadata.typeName, expectedPrefix)
            );
        }
    }

    // 경로 포맷 가져오기
    private String getPathFormat(ImageType imageType) {
        ImageTypeMetadata metadata = getMetadata(imageType);
        String format = switch (imageType) {
            case PROFILE -> imageProperties.getProfilePathFormat();
            case POST -> imageProperties.getPostPathFormat();
            case PRODUCT -> null; // 설정에 없으면 기본값 사용
        };
        
        return (format != null && !format.isBlank()) ? format : metadata.defaultPathFormat;
    }

    // 메타데이터 가져오기
    private ImageTypeMetadata getMetadata(ImageType imageType) {
        ImageTypeMetadata metadata = IMAGE_TYPE_METADATA.get(imageType);
        if (metadata == null) {
            throw new BadRequestException("지원하지 않는 이미지 타입입니다");
        }
        return metadata;
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

    /**
     * ImageType별 메타데이터
     */
    private record ImageTypeMetadata(
        String typeName,              // 타입 이름 (에러 메시지용)
        String resourceIdName,        // 리소스 ID 이름 (에러 메시지용)
        String notFoundMessage,       // 리소스 미존재 에러 메시지
        String defaultPathFormat      // 기본 경로 포맷
    ) {}
}
