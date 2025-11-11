package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.common.Constants;
import com.kakaotechbootcamp.community.common.ImageType;
import com.kakaotechbootcamp.community.config.JwtProperties;
import com.kakaotechbootcamp.community.dto.user.*;
import com.kakaotechbootcamp.community.entity.RefreshToken;
import com.kakaotechbootcamp.community.entity.User;
import com.kakaotechbootcamp.community.exception.*;
import com.kakaotechbootcamp.community.jwt.JwtProvider;
import com.kakaotechbootcamp.community.repository.RefreshTokenRepository;
import com.kakaotechbootcamp.community.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자(User) 도메인 서비스
 * - 회원가입, 조회, 수정, 탈퇴 비즈니스 로직
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ImageUploadService imageUploadService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    /** 토큰 응답 record */
    public record TokenResponse(String accessToken, String refreshToken) {}

    /**
     * 회원가입
     * - 의도: 이메일/닉네임 중복 검사 후 사용자 생성
     * - 프로필 이미지: 파일이 있으면 업로드 후 profileImageKey 설정
     * - 에러: 중복 시 409(Conflict)
     */
    @Transactional
    public ApiResponse<UserResponseDto> create(UserCreateRequestDto request, MultipartFile profileImage) {
        String email = request.getEmail().trim().toLowerCase();
        String nickname = request.getNickname().trim();

        // 이메일 중복 검사 (삭제 대기 중인 계정도 포함)
        if (userRepository.findByEmailIncludingDeleted(email).isPresent()) {
            throw new ConflictException("이미 사용 중인 이메일입니다");
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new ConflictException("이미 사용 중인 닉네임입니다");
        }
        
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(email, encodedPassword, nickname);
        User saved = userRepository.save(user);

        // 프로필 이미지 처리
        if (profileImage != null && !profileImage.isEmpty()) {
            // 프로필 이미지 업로드 및 저장
            com.kakaotechbootcamp.community.dto.image.ImageUploadResponseDto uploadResponse = 
                    imageUploadService.uploadMultipart(ImageType.PROFILE, saved.getId(), profileImage);
            saved.updateProfileImageKey(uploadResponse.objectKey());
            saved = userRepository.save(saved);
        } else if (request.getProfileImageKey() != null && !request.getProfileImageKey().trim().isEmpty()) {
            // 기존 objectKey가 제공된 경우 (하위 호환성)
            String profileKey = request.getProfileImageKey().trim();
            if (!profileKey.isEmpty()) {
                imageUploadService.validateObjectKey(ImageType.PROFILE, profileKey, saved.getId());
                saved.updateProfileImageKey(profileKey);
                saved = userRepository.save(saved);
            }
        }

        return ApiResponse.created(UserResponseDto.from(saved));
    }

    /**
     * 로그인
     * - 의도: 이메일/비밀번호 검증 후 JWT 토큰 발급 및 쿠키 설정
     * - 로직: 소프트 삭제된 사용자도 조회하여 로그인 허용
     * - 에러: 이메일 없음/비밀번호 불일치 시 400(BadRequest)
     */
    @Transactional
    public ApiResponse<UserLoginResponseDto> login(UserLoginRequestDto request, HttpServletResponse response) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIncludingDeleted(email)
                .orElseThrow(() -> new BadRequestException("이메일 또는 비밀번호가 일치하지 않습니다"));

        if (!checkPassword(user, request.getPassword())) {
            throw new BadRequestException("이메일 또는 비밀번호가 일치하지 않습니다");
        }

        // 기존 리프레시 토큰 무효화
        refreshTokenRepository.deleteByUserId(user.getId().longValue());

        // 새로운 토큰 발급 및 저장
        TokenResponse tokenResponse = generateAndSaveTokens(user);

        // 쿠키 추가
        boolean rememberMe = Boolean.TRUE.equals(request.getRememberMe());
        addTokenCookies(response, tokenResponse, rememberMe);

        UserLoginResponseDto loginResponse = new UserLoginResponseDto(
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                UserResponseDto.from(user)
        );

        return ApiResponse.modified(loginResponse);
    }

    /**
     * 로그아웃
     * - 의도: 쿠키를 즉시 만료시키고 DB의 refresh token도 무효화
     */
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키에서 refresh token 추출 (만료된 토큰이어도 문자열은 있음)
        extractRefreshTokenFromCookie(request).ifPresent(refreshTokenString -> {
            // DB에서 refresh token 찾아서 무효화
            refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenString)
                    .ifPresent(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                    });
        });
        
        // 쿠키 즉시 만료
        addTokenCookie(response, Constants.Cookie.ACCESS_TOKEN, null, 0);
        addTokenCookie(response, Constants.Cookie.REFRESH_TOKEN, null, 0);
    }

    /**
     * 회원 정보 조회
     * - 의도: id로 활성 사용자 조회
     * - 에러: 없으면 404(NotFound)
     */
    @Transactional(readOnly = true)
    public ApiResponse<UserResponseDto> getById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));
        return ApiResponse.modified(UserResponseDto.from(user));
    }

    /**
     * 회원 정보 수정
     * - 의도: 닉네임/프로필 부분 수정(선택 입력)
     * - 로직: 닉네임 동일 시 중복검사 생략, 프로필 빈문자→null
     * - 에러: 닉네임 중복 시 409(Conflict)
     */
    @Transactional
    public ApiResponse<UserResponseDto> update(Integer id, UserUpdateRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            String newNickname = request.getNickname().trim();
            if (!newNickname.equals(user.getNickname()) && userRepository.existsByNicknameAndIdNot(newNickname, id)) {
                throw new ConflictException("이미 사용 중인 닉네임입니다");
            }
            user.updateNickname(newNickname);
        }
        if (request.getProfileImageKey() != null) {
            String newProfileKey = request.getProfileImageKey().trim();
            String finalProfileKey = newProfileKey.isEmpty() ? null : newProfileKey;
            
            // 프로필 이미지 objectKey 검증
            if (finalProfileKey != null) {
                imageUploadService.validateObjectKey(ImageType.PROFILE, finalProfileKey, id);
            }
            
            user.updateProfileImageKey(finalProfileKey);
        }
        return ApiResponse.modified(UserResponseDto.from(user));
    }

    /**
     * 회원 탈퇴(소프트 삭제)
     * - 의도: deletedAt 설정으로 비활성화
     */
    @Transactional
    public ApiResponse<Void> delete(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));
        user.softDelete();
        return ApiResponse.deleted(null);
    }

    /**
     * 회원 복구
     * - 의도: deletedAt을 null로 설정하여 계정 복구
     */
    @Transactional
    public ApiResponse<Void> restore(Integer id) {
        User user = userRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));
        
        if (user.getDeletedAt() == null) {
            throw new BadRequestException("이미 활성화된 계정입니다");
        }
        
        user.restore();
        return ApiResponse.modified(null);
    }

    /**
     * 이메일 사용 가능 여부
     * - 반환: true=사용 가능, false=중복
     */
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> isEmailAvailable(String email) {
        String emailLower = email.trim().toLowerCase();
        // 이메일 중복 검사 (삭제 대기 중인 계정도 포함)
        boolean exists = userRepository.findByEmailIncludingDeleted(emailLower).isPresent();
        
        Map<String, Object> result = new HashMap<>();
        result.put("available", !exists);
        
        return ApiResponse.modified(result);
    }

    /**
     * 닉네임 사용 가능 여부
     * - 반환: true=사용 가능, false=중복
     */
    @Transactional(readOnly = true)
    public ApiResponse<Boolean> isNicknameAvailable(String nickname) {
        boolean exists = userRepository.existsByNickname(nickname.trim());
        return ApiResponse.modified(!exists);
    }

    /**
     * 비밀번호 변경
     * - 현재 비밀번호로 검증 후 새 비밀번호 + 확인 비밀번호 검증 후 변경
     */
    @Transactional
    public ApiResponse<Void> updatePassword(Integer id, String newPassword, String confirmPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        String next = newPassword == null ? "" : newPassword.trim();
        if (next.isEmpty()) {
            throw new BadRequestException("새 비밀번호를 입력해주세요");
        }

        String confirm = confirmPassword == null ? "" : confirmPassword.trim();
        if (confirm.isEmpty()) {
            throw new BadRequestException("비밀번호 확인을 입력해주세요");
        }

        if (!next.equals(confirm)) {
            throw new BadRequestException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다");
        }

        // 새 비밀번호가 현재 비밀번호와 동일한지 확인
        if (passwordEncoder.matches(next, user.getPassword())) {
            throw new BadRequestException("이전 비밀번호와 새 비밀번호가 동일합니다");
        }

        String encodedNewPassword = passwordEncoder.encode(next);
        user.updatePassword(encodedNewPassword);
        return ApiResponse.modified(null);
    }


    /**
     * 리프레시 토큰으로 새 액세스 토큰 발급
     * - 의도: 쿠키에서 refresh token을 읽어 새 액세스 토큰만 발급 (refresh token은 유지)
     * - 에러: 토큰 없음/만료/회수됨 시 400(BadRequest)
     */
    @Transactional
    public ApiResponse<TokenResponseDto> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenString = extractRefreshTokenFromCookie(request)
                .orElseThrow(() -> new BadRequestException("리프레시 토큰이 없습니다"));

        var parsedRefreshToken = jwtProvider.parse(refreshTokenString);

        RefreshToken entity = refreshTokenRepository
                .findByTokenAndRevokedFalse(refreshTokenString)
                .orElseThrow(() -> new BadRequestException("유효하지 않은 리프레시 토큰입니다"));

        if (entity.getExpiresAt().isBefore(Instant.now())) {
            entity.setRevoked(true);
            refreshTokenRepository.save(entity);
            throw new BadRequestException("만료된 리프레시 토큰입니다");
        }

        Claims claims = parsedRefreshToken.getBody();
        Long userId = Long.valueOf(claims.getSubject());
        User user = userRepository.findById(userId.intValue())
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다"));

        // refresh token은 유지하고 access token만 새로 발급
        String newAccessToken = jwtProvider.createAccessToken(user.getId().longValue(), JwtProvider.ROLE_USER);

        // access token 쿠키만 갱신
        addTokenCookie(response, Constants.Cookie.ACCESS_TOKEN, newAccessToken, (int) jwtProperties.getAccessTokenTtlSeconds());

        TokenResponseDto tokenResponse = new TokenResponseDto(newAccessToken, refreshTokenString);
        return ApiResponse.modified(tokenResponse);
    }

    /** Access, Refresh 토큰을 새로 발급하고 DB에 저장 */
    private TokenResponse generateAndSaveTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId().longValue(), JwtProvider.ROLE_USER);
        String refreshToken = jwtProvider.createRefreshToken(user.getId().longValue());

        RefreshToken refreshEntity = new RefreshToken();
        refreshEntity.setUserId(user.getId().longValue());
        refreshEntity.setToken(refreshToken);
        refreshEntity.setExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshTokenTtlSeconds()));
        refreshEntity.setRevoked(false);
        refreshTokenRepository.save(refreshEntity);

        return new TokenResponse(accessToken, refreshToken);
    }

    /** AccessToken + RefreshToken 쿠키를 한번에 추가 */
    private void addTokenCookies(HttpServletResponse response, TokenResponse tokenResponse, boolean rememberMe) {
        addTokenCookie(response, Constants.Cookie.ACCESS_TOKEN, tokenResponse.accessToken(), (int) jwtProperties.getAccessTokenTtlSeconds());
        int refreshMaxAge = rememberMe ? (int) jwtProperties.getRefreshTokenTtlSeconds() : -1; // -1: 세션 쿠키
        addTokenCookie(response, Constants.Cookie.REFRESH_TOKEN, tokenResponse.refreshToken(), refreshMaxAge);
    }

    /** 공통 쿠키 생성 로직 */
    private void addTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    /** 비밀번호 검증 */
    private boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    /** 쿠키에서 refresh token 추출 */
    private java.util.Optional<String> extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return java.util.Optional.empty();
        }
        return java.util.Arrays.stream(cookies)
                .filter(cookie -> Constants.Cookie.REFRESH_TOKEN.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
