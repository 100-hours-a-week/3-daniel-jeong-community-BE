package com.kakaotechbootcamp.community.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * 비밀번호 변경/재설정 요청 DTO
 * - 의도: 새 비밀번호 + 확인 비밀번호 검증 후 변경
 */
@Getter
public class PasswordUpdateRequestDto {
    
    @NotBlank(message = "새 비밀번호를 입력해주세요")
    private String newPassword;
    
    @NotBlank(message = "비밀번호 확인을 입력해주세요")
    private String confirmPassword;
}

