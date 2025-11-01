package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.dto.user.UserLoginRequestDto;
import com.kakaotechbootcamp.community.dto.user.UserLoginResponseDto;
import com.kakaotechbootcamp.community.entity.User;
import com.kakaotechbootcamp.community.exception.BadRequestException;
import com.kakaotechbootcamp.community.exception.NotFoundException;
import com.kakaotechbootcamp.community.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 세션 기반 인증 서비스
 * - 로그인, 로그아웃, 세션 검증 로직
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SessionService {

    // 세션에 저장할 사용자 정보 키 이름
    private static final String SESSION_USER_ID = "userId";
    private static final String SESSION_USER_EMAIL = "userEmail";

    private final UserRepository userRepository;

    /**
     * 로그인
     * - 의도: 이메일/비밀번호 검증 후 세션 생성
     * - 에러: 사용자 없음 404, 비밀번호 불일치 400
     */
    @Transactional
    public ApiResponse<UserLoginResponseDto> login(UserLoginRequestDto request, HttpSession session) {
        String email = request.getEmail().trim().toLowerCase();
        String password = request.getPassword();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("이메일 또는 비밀번호가 올바르지 않습니다"));

        // 비밀번호 검증 (현재는 평문 저장이므로 평문 비교)
        // 실제 프로덕션에서는 BCrypt 등 암호화된 비밀번호 비교 필요
        if (!password.equals(user.getPassword())) {
            throw new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        // 세션에 사용자 정보 저장
        session.setAttribute(SESSION_USER_ID, user.getId());
        session.setAttribute(SESSION_USER_EMAIL, user.getEmail());

        return ApiResponse.modified(UserLoginResponseDto.from(user));
    }

    /**
     * 로그아웃
     * - 의도: 세션 무효화
     */
    public void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * 현재 로그인한 사용자 ID 조회
     * - 의도: 세션에서 사용자 ID 추출
     * - 반환: 로그인하지 않았으면 null
     */
    public Integer getCurrentUserId(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (Integer) session.getAttribute(SESSION_USER_ID);
    }

    /**
     * 현재 로그인한 사용자 이메일 조회
     * - 의도: 세션에서 사용자 이메일 추출 (DB 조회 없음)
     * - 반환: 로그인하지 않았으면 null
     */
    public String getCurrentUserEmail(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute(SESSION_USER_EMAIL);
    }

    /**
     * 현재 로그인한 사용자 조회
     * - 의도: 세션에서 사용자 정보 조회
     * - 에러: 세션 없음 또는 사용자 없음 404
     */
    @Transactional(readOnly = true)
    public User getCurrentUser(HttpSession session) {
        Integer userId = getCurrentUserId(session);
        if (userId == null) {
            throw new NotFoundException("로그인이 필요합니다");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));
    }

    /**
     * 세션 검증
     * - 의도: 세션이 유효한지 확인
     * - 반환: 유효한 세션이면 true
     */
    public boolean isAuthenticated(HttpSession session) {
        if (session == null) {
            return false;
        }
        Integer userId = (Integer) session.getAttribute(SESSION_USER_ID);
        return userId != null;
    }
}
