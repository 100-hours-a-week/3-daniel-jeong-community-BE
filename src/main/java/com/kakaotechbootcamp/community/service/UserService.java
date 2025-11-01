package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.common.ImageType;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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

    @Value("${jwt.access-token-ttl-seconds}")
    private long accessTokenTtlSeconds;

    @Value("${jwt.refresh-token-ttl-seconds}")
    private long refreshTokenTtlSeconds;

    /** 토큰 응답 record */
    public record TokenResponse(String accessToken, String refreshToken) {}

    /**
     * 회원가입
     * - 의도: 이메일/닉네임 중복 검사 후 사용자 생성
     * - 에러: 중복 시 409(Conflict)
     */
    @Transactional
    public ApiResponse<UserResponseDto> create(UserCreateRequestDto request) {
        String email = request.getEmail().trim().toLowerCase();
        String nickname = request.getNickname().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("이미 사용 중인 이메일입니다");
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new ConflictException("이미 사용 중인 닉네임입니다");
        }
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(email, encodedPassword, nickname);
        User saved = userRepository.save(user);

        // 프로필 이미지 처리 (있으면 검증 후 설정)
        if (request.getProfileImageKey() != null) {
            String profileKey = request.getProfileImageKey().trim();
            
            // 프로필 이미지 objectKey 검증
            if (!profileKey.isEmpty()) {
                imageUploadService.validateObjectKey(ImageType.PROFILE, profileKey, saved.getId());
                saved.updateProfileImageKey(profileKey);
            }
        }

        return ApiResponse.created(UserResponseDto.from(saved));
    }

    /**
     * 로그인
     * - 의도: 이메일/비밀번호 검증 후 JWT 토큰 발급 및 쿠키 설정
     * - 에러: 이메일 없음/비밀번호 불일치 시 400(BadRequest)
     */
    @Transactional
    public ApiResponse<UserLoginResponseDto> login(UserLoginRequestDto request, HttpServletResponse response) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("이메일 또는 비밀번호가 일치하지 않습니다"));

        if (!checkPassword(user, request.getPassword())) {
            throw new BadRequestException("이메일 또는 비밀번호가 일치하지 않습니다");
        }

        // 기존 리프레시 토큰 무효화
        refreshTokenRepository.deleteByUserId(user.getId().longValue());

        // 새로운 토큰 발급 및 저장
        TokenResponse tokenResponse = generateAndSaveTokens(user);

        // 쿠키 추가
        addTokenCookies(response, tokenResponse);

        UserLoginResponseDto loginResponse = new UserLoginResponseDto(
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                UserResponseDto.from(user)
        );

        return ApiResponse.modified(loginResponse);
    }

    /**
     * 로그아웃
     * - 의도: 쿠키를 즉시 만료시켜 로그아웃 처리
     */
    public void logout(HttpServletResponse response) {
        // 쿠키 즉시 만료
        addTokenCookie(response, "accessToken", null, 0);
        addTokenCookie(response, "refreshToken", null, 0);
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
     * - 의도: deletedAt 설정로 비활성화
     */
    @Transactional
    public ApiResponse<Void> delete(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));
        user.softDelete();
        return ApiResponse.deleted(null);
    }

    /**
     * 이메일 사용 가능 여부
     * - 반환: true=사용 가능, false=중복
     */
    @Transactional(readOnly = true)
    public ApiResponse<Boolean> isEmailAvailable(String email) {
        boolean exists = userRepository.existsByEmail(email.trim().toLowerCase());
        return ApiResponse.modified(!exists);
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
     * - 의도: 현재/신규 비밀번호 검증 후 업데이트
     * - 에러: 공백/동일/현재 불일치 시 400(BadRequest)
     */
    @Transactional
    public ApiResponse<Void> updatePassword(Integer id, String currentPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        String cur = currentPassword == null ? "" : currentPassword.trim();
        String next = newPassword == null ? "" : newPassword.trim();
        if (cur.isEmpty() || next.isEmpty()) {
            throw new BadRequestException("비밀번호를 모두 입력해주세요");
        }
        if (cur.equals(next)) {
            throw new BadRequestException("이전 비밀번호와 새 비밀번호가 동일합니다");
        }
        if (!passwordEncoder.matches(cur, user.getPassword())) {
            throw new BadRequestException("현재 비밀번호가 일치하지 않습니다");
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
        String newAccessToken = jwtProvider.createAccessToken(user.getId().longValue(), "USER");

        // access token 쿠키만 갱신
        addTokenCookie(response, "accessToken", newAccessToken, (int) accessTokenTtlSeconds);

        TokenResponseDto tokenResponse = new TokenResponseDto(newAccessToken, refreshTokenString);
        return ApiResponse.modified(tokenResponse);
    }

    /** Access / Refresh 토큰을 새로 발급하고 DB에 저장 */
    private TokenResponse generateAndSaveTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId().longValue(), "USER");
        String refreshToken = jwtProvider.createRefreshToken(user.getId().longValue());

        RefreshToken refreshEntity = new RefreshToken();
        refreshEntity.setUserId(user.getId().longValue());
        refreshEntity.setToken(refreshToken);
        refreshEntity.setExpiresAt(Instant.now().plusSeconds(refreshTokenTtlSeconds));
        refreshEntity.setRevoked(false);
        refreshTokenRepository.save(refreshEntity);

        return new TokenResponse(accessToken, refreshToken);
    }

    /** AccessToken + RefreshToken 쿠키를 한번에 추가 */
    private void addTokenCookies(HttpServletResponse response, TokenResponse tokenResponse) {
        addTokenCookie(response, "accessToken", tokenResponse.accessToken(), (int) accessTokenTtlSeconds);
        addTokenCookie(response, "refreshToken", tokenResponse.refreshToken(), (int) refreshTokenTtlSeconds);
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
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
