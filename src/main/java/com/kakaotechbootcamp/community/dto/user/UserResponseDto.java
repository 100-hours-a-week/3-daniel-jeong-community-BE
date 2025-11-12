package com.kakaotechbootcamp.community.dto.user;

import com.kakaotechbootcamp.community.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 정보 응답 DTO
 * - 의도: 외부로 노출할 사용자 정보 형태를 고정
 */
@Getter
@AllArgsConstructor
public class UserResponseDto {

	private final Integer id;
	private final String email;
	private final String nickname;
	private final String profileImageKey;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;
	private final LocalDateTime deletedAt;

    /**
     * 엔티티 → 응답 DTO 변환
     * - 로직: 필요한 필드만 추출 (엔티티 직접 노출 금지)
     */
    public static UserResponseDto from(User user) {
        return new UserResponseDto(
				user.getId(),
				user.getEmail(),
				user.getNickname(),
				user.getProfileImageKey(),
				user.getCreatedAt(),
				user.getUpdatedAt(),
				user.getDeletedAt()
		);
	}
}
