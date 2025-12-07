package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ImageProperties;
import com.kakaotechbootcamp.community.common.ImageType;
import com.kakaotechbootcamp.community.exception.BadRequestException;
import com.kakaotechbootcamp.community.exception.NotFoundException;
import com.kakaotechbootcamp.community.repository.PostRepository;
import com.kakaotechbootcamp.community.repository.ProductRepository;
import com.kakaotechbootcamp.community.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * 이미지 업로드 서비스 보안 테스트
 */
@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ImageProperties imageProperties;

    private ImageUploadService imageUploadService;

    @BeforeEach
    void setUp() {
        imageUploadService = new ImageUploadService(
                userRepository,
                postRepository,
                productRepository,
                imageProperties
        );
    }

    @Nested
    @DisplayName("이미지 확장자 검증")
    class ImageExtensionValidationTest {

        @Test
        @DisplayName("허용된 확장자만 업로드 가능")
        void generateObjectKey_whenAllowedExtension_succeeds() {
            // given
            Set<String> allowedExtensions = Set.of("jpeg", "jpg", "png", "gif", "webp");
            given(imageProperties.getAllowedExtensionSet()).willReturn(allowedExtensions);
            given(imageProperties.getProfilePathFormat()).willReturn("user/%d/profile/%s");

            // when
            String objectKey = imageUploadService.generateObjectKey(ImageType.PROFILE, 1, "test.jpg");

            // then
            assertThat(objectKey).isNotNull();
            assertThat(objectKey).contains("user/1/profile");
        }

        @Test
        @DisplayName("허용되지 않은 확장자 업로드 시 예외 발생")
        void generateObjectKey_whenInvalidExtension_throwsBadRequestException() {
            // given
            Set<String> allowedExtensions = Set.of("jpeg", "jpg", "png", "gif", "webp");
            given(imageProperties.getAllowedExtensionSet()).willReturn(allowedExtensions);

            // when // then
            assertThatThrownBy(() -> imageUploadService.generateObjectKey(ImageType.PROFILE, 1, "test.exe"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("지원하지 않는 이미지 확장자");
        }
    }

    @Nested
    @DisplayName("리소스 존재 검증")
    class ResourceExistenceValidationTest {

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 프로필 이미지 업로드 시 예외 발생")
        void validateResourceExists_whenUserNotExists_throwsNotFoundException() {
            // given
            given(userRepository.existsById(999)).willReturn(false);

            // when // then
            assertThatThrownBy(() -> imageUploadService.validateResourceExists(ImageType.PROFILE, 999))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("존재하는 사용자 ID로 프로필 이미지 업로드 시 성공")
        void validateResourceExists_whenUserExists_succeeds() {
            // given
            given(userRepository.existsById(1)).willReturn(true);

            // when
            imageUploadService.validateResourceExists(ImageType.PROFILE, 1);

            // then
            then(userRepository).should(times(1)).existsById(1);
        }
    }

    @Nested
    @DisplayName("경로 보안")
    class PathSecurityTest {

        @Test
        @DisplayName("objectKey는 지정된 경로 포맷을 따라야 함")
        void validateObjectKey_whenInvalidPath_throwsBadRequestException() {
            // given
            given(imageProperties.getProfilePathFormat()).willReturn("user/%d/profile/%s");
            given(userRepository.existsById(1)).willReturn(true);
            String invalidObjectKey = "invalid/path/image.jpg";

            // when // then
            assertThatThrownBy(() -> imageUploadService.validateObjectKey(ImageType.PROFILE, invalidObjectKey, 1))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("로 시작해야 합니다");
        }

        @Test
        @DisplayName("경로 조작 시도 차단 - 경로 분리자 제거")
        void generateObjectKey_whenPathTraversal_sanitizesFilename() {
            // given
            Set<String> allowedExtensions = Set.of("jpeg", "jpg", "png", "gif", "webp");
            given(imageProperties.getAllowedExtensionSet()).willReturn(allowedExtensions);
            given(imageProperties.getProfilePathFormat()).willReturn("user/%d/profile/%s");

            // when
            String objectKey = imageUploadService.generateObjectKey(ImageType.PROFILE, 1, "../../../etc/passwd.jpg");

            // then
            assertThat(objectKey).isNotNull();
            assertThat(objectKey).contains("user/1/profile");
            assertThat(objectKey).doesNotContain("../");
        }
    }
}

