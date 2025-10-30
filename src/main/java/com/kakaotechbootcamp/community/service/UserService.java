package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.dto.user.UserCreateRequestDto;
import com.kakaotechbootcamp.community.dto.user.UserResponseDto;
import com.kakaotechbootcamp.community.dto.user.UserUpdateRequestDto;
import com.kakaotechbootcamp.community.entity.User;
import com.kakaotechbootcamp.community.exception.ConflictException;
import com.kakaotechbootcamp.community.exception.NotFoundException;
import com.kakaotechbootcamp.community.repository.UserRepository;

import org.springframework.stereotype.Service;
import com.kakaotechbootcamp.community.exception.BadRequestException;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자(User) 도메인 서비스
 * - 회원가입, 조회, 수정, 탈퇴 비즈니스 로직
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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
        User user = new User(email, request.getPassword(), nickname);
        User saved = userRepository.save(user);
        return ApiResponse.created(UserResponseDto.from(saved));
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
            user.updateProfileImageKey(newProfileKey.isEmpty() ? null : newProfileKey);
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
        if (!cur.equals(user.getPassword())) {
            throw new BadRequestException("현재 비밀번호가 일치하지 않습니다");
        }
        user.updatePassword(next);
        return ApiResponse.modified(null);
    }
}
