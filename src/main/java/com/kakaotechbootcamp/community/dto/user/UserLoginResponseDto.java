package com.kakaotechbootcamp.community.dto.user;

import com.kakaotechbootcamp.community.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 로그인 응답 DTO
 * - 의도: 로그인 성공 시 사용자 정보 반환
 */
@Getter
@AllArgsConstructor
public class UserLoginResponseDto {

    private final Integer id;
    private final String email;
    private final String nickname;

    public static UserLoginResponseDto from(User user) {
        return new UserLoginResponseDto(user.getId(), user.getEmail(), user.getNickname());
    }
}
