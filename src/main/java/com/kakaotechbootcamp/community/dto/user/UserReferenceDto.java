package com.kakaotechbootcamp.community.dto.user;

import com.kakaotechbootcamp.community.dto.image.ImageResponseDto;
import com.kakaotechbootcamp.community.entity.User;

/**
 * 사용자 참조 DTO
 * - 의도: 게시글/댓글에서 사용자 요약 정보(id, nickname, 이미지)를 제공
 * - 주의: 민감 정보(이메일/비밀번호 등)는 포함하지 않음
 */
public record UserReferenceDto(
        Integer id,
        String nickname,
        ImageResponseDto image
) {
    public static UserReferenceDto from(User user) {
        return new UserReferenceDto(
                user.getId(),
                user.getNickname(),
                ImageResponseDto.of(user.getProfileImageKey())
        );
    }
}
