package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.entity.User;
import com.kakaotechbootcamp.community.exception.BadRequestException;
import com.kakaotechbootcamp.community.exception.NotFoundException;
import com.kakaotechbootcamp.community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserService 테스트
 * - 회원가입, 조회, 수정, 탈퇴 등 사용자 관련 비즈니스 로직 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("이메일 중복 검증 - 사용 가능한 이메일")
    void isEmailAvailable_whenEmailNotExists_returnsTrue() {
        ApiResponse<Map<String, Object>> response = userService.isEmailAvailable("newuser@example.com");

        assertTrue(response.isSuccess());
        assertTrue((Boolean) response.getData().get("available"));
    }

    @Test
    @DisplayName("이메일 중복 검증 - 이미 존재하는 이메일")
    void isEmailAvailable_whenEmailExists_returnsFalse() {
        createAndSaveUser("test@example.com", "testuser");

        ApiResponse<Map<String, Object>> response = userService.isEmailAvailable("test@example.com");

        assertTrue(response.isSuccess());
        assertFalse((Boolean) response.getData().get("available"));
    }

    @Test
    @DisplayName("닉네임 중복 검증 - 사용 가능한 닉네임")
    void isNicknameAvailable_whenNicknameNotExists_returnsTrue() {
        ApiResponse<Boolean> response = userService.isNicknameAvailable("newnickname");

        assertTrue(response.isSuccess());
        assertTrue(response.getData());
    }

    @Test
    @DisplayName("닉네임 중복 검증 - 이미 존재하는 닉네임")
    void isNicknameAvailable_whenNicknameExists_returnsFalse() {
        createAndSaveUser("test2@example.com", "existingnick");

        ApiResponse<Boolean> response = userService.isNicknameAvailable("existingnick");

        assertTrue(response.isSuccess());
        assertFalse(response.getData());
    }

    @Test
    @DisplayName("회원 정보 조회 - 정상 케이스")
    void getById_whenUserExists_returnsUser() {
        User user = createAndSaveUser("user@example.com", "testuser");

        ApiResponse<?> response = userService.getById(user.getId());

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
    }

    @Test
    @DisplayName("회원 정보 조회 - 존재하지 않는 사용자")
    void getById_whenUserNotExists_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> userService.getById(999));
    }

    @Test
    @DisplayName("회원 탈퇴 - 소프트 삭제")
    void delete_whenUserExists_softDeletesUser() {
        User user = createAndSaveUser("user@example.com", "testuser");

        ApiResponse<?> response = userService.delete(user.getId());

        assertTrue(response.isSuccess());
        assertTrue(userRepository.findByEmail("user@example.com").isEmpty());
    }

    @Test
    @DisplayName("회원 복구 - 정상 케이스")
    void restore_whenUserDeleted_restoresUser() {
        User user = createAndSaveUser("user@example.com", "testuser");
        userService.delete(user.getId());

        ApiResponse<?> response = userService.restore(user.getId());

        assertTrue(response.isSuccess());
        assertTrue(userRepository.findByEmail("user@example.com").isPresent());
    }

    @Test
    @DisplayName("회원 복구 - 이미 활성화된 계정")
    void restore_whenUserNotDeleted_throwsBadRequestException() {
        User user = createAndSaveUser("user@example.com", "testuser");

        assertThrows(BadRequestException.class, () -> userService.restore(user.getId()));
    }

    @Test
    @DisplayName("비밀번호 변경 - 정상 케이스")
    void updatePassword_whenValidRequest_updatesPassword() {
        User user = createAndSaveUser("user@example.com", "testuser");
        String oldPassword = user.getPassword();

        ApiResponse<?> response = userService.updatePassword(user.getId(), "NewPass123!", "NewPass123!");

        assertTrue(response.isSuccess());
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertNotEquals(oldPassword, updated.getPassword());
        assertTrue(passwordEncoder.matches("NewPass123!", updated.getPassword()));
    }

    @Test
    @DisplayName("비밀번호 변경 - 비밀번호 불일치")
    void updatePassword_whenPasswordsNotMatch_throwsBadRequestException() {
        User user = createAndSaveUser("user@example.com", "testuser");

        assertThrows(BadRequestException.class,
                () -> userService.updatePassword(user.getId(), "NewPass123!", "Different123!"));
    }

    @Test
    @DisplayName("비밀번호 변경 - 현재 비밀번호와 동일")
    void updatePassword_whenSameAsCurrent_throwsBadRequestException() {
        User user = createAndSaveUser("user@example.com", "testuser");
        String currentPassword = "testPassword123!";

        assertThrows(BadRequestException.class,
                () -> userService.updatePassword(user.getId(), currentPassword, currentPassword));
    }

    // 테스트용 사용자 생성 및 저장
    private User createAndSaveUser(String email, String nickname) {
        User user = new User(email, passwordEncoder.encode("testPassword123!"), nickname);
        return userRepository.save(user);
    }
}