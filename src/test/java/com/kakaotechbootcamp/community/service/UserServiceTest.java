package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.common.Constants;
import com.kakaotechbootcamp.community.common.ImageProperties;
import com.kakaotechbootcamp.community.common.ImageType;
import com.kakaotechbootcamp.community.config.JwtProperties;
import com.kakaotechbootcamp.community.dto.user.UserCreateRequestDto;
import com.kakaotechbootcamp.community.dto.user.UserLoginRequestDto;
import com.kakaotechbootcamp.community.entity.RefreshToken;
import com.kakaotechbootcamp.community.entity.User;
import com.kakaotechbootcamp.community.exception.BadRequestException;
import com.kakaotechbootcamp.community.exception.ConflictException;
import com.kakaotechbootcamp.community.jwt.JwtProvider;
import com.kakaotechbootcamp.community.repository.RefreshTokenRepository;
import com.kakaotechbootcamp.community.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * 사용자 서비스 핵심 보안 및 비즈니스 로직 테스트
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ImageUploadService imageUploadService;

    @Mock
    private S3Service s3Service;

    @Mock
    private ImageProperties imageProperties;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Nested
    @DisplayName("회원가입")
    class CreateMemberTest {

        @Test
        @DisplayName("비밀번호는 반드시 암호화되어 저장되어야 한다")
        void createMember_passwordMustBeEncrypted() throws Exception {
            // given
            UserCreateRequestDto request = createRequest("test@example.com", "Password123!", "testuser");
            String encodedPassword = "$2a$10$encodedPasswordHash";

            given(userRepository.findByEmailIncludingDeleted("test@example.com")).willReturn(Optional.empty());
            given(userRepository.countByNicknameIncludingDeleted("testuser")).willReturn(0L);
            given(passwordEncoder.encode("Password123!")).willReturn(encodedPassword);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User user = invocation.getArgument(0);
                ReflectionTestUtils.setField(user, "id", 1);
                return user;
            });

            // when
            ApiResponse<?> response = userService.create(request, null);

            // then
            assertThat(response.isSuccess()).isTrue();
            then(passwordEncoder).should(times(1)).encode("Password123!");
        }

        @Test
        @DisplayName("이미 존재하는 이메일로 가입 시도 시 예외 발생")
        void createMember_whenEmailExists_throwsConflictException() throws Exception {
            // given
            UserCreateRequestDto request = createRequest("duplicate@example.com", "Password123!", "testuser");
            User existingUser = new User("duplicate@example.com", "encoded", "existing");

            given(userRepository.findByEmailIncludingDeleted("duplicate@example.com")).willReturn(Optional.of(existingUser));

            // when // then
            assertThatThrownBy(() -> userService.create(request, null))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("이미 사용 중인 이메일입니다");

            then(userRepository).should(never()).save(any(User.class));
        }

        @Test
        @DisplayName("이미 존재하는 닉네임으로 가입 시도 시 예외 발생")
        void createMember_whenNicknameExists_throwsConflictException() throws Exception {
            // given
            UserCreateRequestDto request = createRequest("test@example.com", "Password123!", "duplicatenick");

            given(userRepository.findByEmailIncludingDeleted("test@example.com")).willReturn(Optional.empty());
            given(userRepository.countByNicknameIncludingDeleted("duplicatenick")).willReturn(1L);

            // when // then
            assertThatThrownBy(() -> userService.create(request, null))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("이미 사용 중인 닉네임입니다");

            then(userRepository).should(never()).save(any(User.class));
        }

        @Test
        @DisplayName("프로필 이미지 크기 초과 시 예외 발생")
        void createMember_whenImageSizeExceeds_throwsBadRequestException() throws Exception {
            // given
            UserCreateRequestDto request = createRequest("test@example.com", "Password123!", "testuser");
            MultipartFile oversizedImage = mock(MultipartFile.class);

            given(userRepository.findByEmailIncludingDeleted("test@example.com")).willReturn(Optional.empty());
            given(userRepository.countByNicknameIncludingDeleted("testuser")).willReturn(0L);
            given(passwordEncoder.encode("Password123!")).willReturn("encoded");
            given(imageProperties.getMaxSizeBytes()).willReturn(5242880L);
            given(oversizedImage.isEmpty()).willReturn(false);
            given(oversizedImage.getSize()).willReturn(5242881L);

            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User user = invocation.getArgument(0);
                ReflectionTestUtils.setField(user, "id", 1);
                return user;
            });

            // when // then
            assertThatThrownBy(() -> userService.create(request, oversizedImage))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("이미지 최대 크기");
        }

        @Test
        @DisplayName("허용되지 않은 이미지 확장자 시 예외 발생")
        void createMember_whenInvalidImageExtension_throwsBadRequestException() throws Exception {
            // given
            UserCreateRequestDto request = createRequest("test@example.com", "Password123!", "testuser");
            MultipartFile invalidImage = mock(MultipartFile.class);

            given(userRepository.findByEmailIncludingDeleted("test@example.com")).willReturn(Optional.empty());
            given(userRepository.countByNicknameIncludingDeleted("testuser")).willReturn(0L);
            given(passwordEncoder.encode("Password123!")).willReturn("encoded");
            given(imageProperties.getMaxSizeBytes()).willReturn(5242880L);
            given(invalidImage.isEmpty()).willReturn(false);
            given(invalidImage.getSize()).willReturn(1000L);
            given(invalidImage.getContentType()).willReturn("image/exe");
            given(imageProperties.extractExtensionFromContentType("image/exe")).willReturn("exe");
            given(imageProperties.getAllowedExtensionSet()).willReturn(java.util.Set.of("jpeg", "jpg", "png", "gif", "webp"));

            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User user = invocation.getArgument(0);
                ReflectionTestUtils.setField(user, "id", 1);
                return user;
            });

            // when // then
            assertThatThrownBy(() -> userService.create(request, invalidImage))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("지원하지 않는 이미지 형식");
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class UpdatePasswordTest {

        @Test
        @DisplayName("이전 비밀번호와 동일한 비밀번호로 변경 시도 시 예외 발생")
        void updatePassword_whenSameAsOldPassword_throwsBadRequestException() {
            // given
            Integer userId = 1;
            User user = new User("test@example.com", "$2a$10$encodedPassword", "testuser");
            ReflectionTestUtils.setField(user, "id", userId);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Password123!", user.getPassword())).willReturn(true);

            // when // then
            assertThatThrownBy(() -> userService.updatePassword(userId, "Password123!", "Password123!"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("이전 비밀번호와 새 비밀번호가 동일합니다");
        }

        @Test
        @DisplayName("새 비밀번호와 확인 비밀번호가 일치하지 않으면 예외 발생")
        void updatePassword_whenPasswordsNotMatch_throwsBadRequestException() {
            // given
            Integer userId = 1;
            User user = new User("test@example.com", "$2a$10$encodedPassword", "testuser");
            ReflectionTestUtils.setField(user, "id", userId);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when // then
            assertThatThrownBy(() -> userService.updatePassword(userId, "NewPassword123!", "DifferentPassword123!"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("새 비밀번호와 비밀번호 확인이 일치하지 않습니다");
        }

        @Test
        @DisplayName("비밀번호 변경 성공 - 새 비밀번호는 암호화되어 저장")
        void updatePassword_whenValid_succeeds() {
            // given
            Integer userId = 1;
            User user = new User("test@example.com", "$2a$10$oldPassword", "testuser");
            ReflectionTestUtils.setField(user, "id", userId);
            String newPassword = "NewPassword123!";
            String encodedNewPassword = "$2a$10$newEncodedPassword";

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(newPassword, user.getPassword())).willReturn(false);
            given(passwordEncoder.encode(newPassword)).willReturn(encodedNewPassword);

            // when
            ApiResponse<?> response = userService.updatePassword(userId, newPassword, newPassword);

            // then
            assertThat(response.isSuccess()).isTrue();
            then(passwordEncoder).should(times(1)).encode(newPassword);
        }
    }

    @Nested
    @DisplayName("로그인")
    class LoginTest {

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시도 시 예외 발생")
        void login_whenEmailNotExists_throwsBadRequestException() throws Exception {
            // given
            UserLoginRequestDto request = createLoginRequest("nonexistent@example.com", "Password123!", false);

            given(userRepository.findByEmailIncludingDeleted("nonexistent@example.com")).willReturn(Optional.empty());

            // when // then
            assertThatThrownBy(() -> userService.login(request, httpServletResponse))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("이메일 또는 비밀번호가 일치하지 않습니다");
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 시도 시 예외 발생")
        void login_whenWrongPassword_throwsBadRequestException() throws Exception {
            // given
            UserLoginRequestDto request = createLoginRequest("test@example.com", "WrongPassword123!", false);
            User user = new User("test@example.com", "$2a$10$encodedPassword", "testuser");
            ReflectionTestUtils.setField(user, "id", 1);

            given(userRepository.findByEmailIncludingDeleted("test@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("WrongPassword123!", user.getPassword())).willReturn(false);

            // when // then
            assertThatThrownBy(() -> userService.login(request, httpServletResponse))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("이메일 또는 비밀번호가 일치하지 않습니다");
        }

        @Test
        @DisplayName("올바른 이메일과 비밀번호로 로그인 시 토큰 발급")
        void login_whenCorrectCredentials_returnsTokens() throws Exception {
            // given
            UserLoginRequestDto request = createLoginRequest("test@example.com", "Password123!", false);
            User user = new User("test@example.com", "$2a$10$encodedPassword", "testuser");
            ReflectionTestUtils.setField(user, "id", 1);

            given(userRepository.findByEmailIncludingDeleted("test@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Password123!", user.getPassword())).willReturn(true);
            doNothing().when(refreshTokenRepository).deleteByUserId(1L);
            given(jwtProvider.createAccessToken(1L, JwtProvider.ROLE_USER)).willReturn("accessToken");
            given(jwtProvider.createRefreshToken(1L)).willReturn("refreshToken");
            given(jwtProperties.getAccessTokenTtlSeconds()).willReturn(3600L);
            given(jwtProperties.getRefreshTokenTtlSeconds()).willReturn(86400L);
            given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            ApiResponse<?> response = userService.login(request, httpServletResponse);

            // then
            assertThat(response.isSuccess()).isTrue();
            then(jwtProvider).should(times(1)).createAccessToken(1L, JwtProvider.ROLE_USER);
            then(jwtProvider).should(times(1)).createRefreshToken(1L);
        }
    }

    @Nested
    @DisplayName("Refresh Token")
    class RefreshTokenTest {

        @Test
        @DisplayName("리프레시 토큰이 없으면 예외 발생")
        void refresh_whenTokenNotExists_throwsBadRequestException() {
            // given
            given(httpServletRequest.getCookies()).willReturn(null);

            // when // then
            assertThatThrownBy(() -> userService.refresh(httpServletRequest, httpServletResponse))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("리프레시 토큰이 없습니다");
        }

        @Test
        @DisplayName("만료된 리프레시 토큰으로 갱신 시도 시 예외 발생")
        void refresh_whenTokenExpired_throwsBadRequestException() {
            // given
            String refreshTokenString = "expiredToken";
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken(refreshTokenString);
            refreshToken.setExpiresAt(java.time.Instant.now().minusSeconds(3600)); // 1시간 전 만료
            refreshToken.setRevoked(false);

            Cookie cookie = new Cookie(Constants.Cookie.REFRESH_TOKEN, refreshTokenString);
            given(httpServletRequest.getCookies()).willReturn(new Cookie[]{cookie});
            given(refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenString)).willReturn(Optional.of(refreshToken));
            given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when // then
            assertThatThrownBy(() -> userService.refresh(httpServletRequest, httpServletResponse))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("만료된 리프레시 토큰입니다");
        }

        @Test
        @DisplayName("유효한 리프레시 토큰으로 새 액세스 토큰 발급 성공")
        void refresh_whenValidToken_returnsNewAccessToken() {
            // given
            String refreshTokenString = "validRefreshToken";
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken(refreshTokenString);
            refreshToken.setExpiresAt(java.time.Instant.now().plusSeconds(3600)); // 1시간 후 만료
            refreshToken.setRevoked(false);
            refreshToken.setUserId(1L);

            User user = new User("test@example.com", "encoded", "testuser");
            ReflectionTestUtils.setField(user, "id", 1);

            Cookie cookie = new Cookie(Constants.Cookie.REFRESH_TOKEN, refreshTokenString);
            given(httpServletRequest.getCookies()).willReturn(new Cookie[]{cookie});
            given(refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenString)).willReturn(Optional.of(refreshToken));
            given(userRepository.findById(1)).willReturn(Optional.of(user));
            given(jwtProvider.createAccessToken(1L, JwtProvider.ROLE_USER)).willReturn("newAccessToken");
            given(jwtProperties.getAccessTokenTtlSeconds()).willReturn(3600L);

            io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
            given(claims.getSubject()).willReturn("1");
            io.jsonwebtoken.Jws<io.jsonwebtoken.Claims> jws = mock(io.jsonwebtoken.Jws.class);
            given(jws.getBody()).willReturn(claims);
            given(jwtProvider.parse(refreshTokenString)).willReturn(jws);

            // when
            ApiResponse<?> response = userService.refresh(httpServletRequest, httpServletResponse);

            // then
            assertThat(response.isSuccess()).isTrue();
            then(jwtProvider).should(times(1)).createAccessToken(1L, JwtProvider.ROLE_USER);
        }
    }

    private UserCreateRequestDto createRequest(String email, String password, String nickname) throws Exception {
        UserCreateRequestDto request = new UserCreateRequestDto();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        ReflectionTestUtils.setField(request, "confirmPassword", password);
        ReflectionTestUtils.setField(request, "nickname", nickname);
        return request;
    }

    private UserLoginRequestDto createLoginRequest(String email, String password, Boolean rememberMe) throws Exception {
        UserLoginRequestDto request = new UserLoginRequestDto();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        ReflectionTestUtils.setField(request, "rememberMe", rememberMe);
        return request;
    }
}

