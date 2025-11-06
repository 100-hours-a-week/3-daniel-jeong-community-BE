package com.kakaotechbootcamp.community.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 비밀번호 재설정 인증번호 발송 요청 DTO
 * - 의도: 이메일로 인증번호 발송 요청
 */
@Getter
public class PasswordResetRequestDto {
    
    @NotBlank(message = "이메일을 입력해주세요")
    @Email
    @Size(max = 320)
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{1,}$", message = "올바른 이메일 형식이 아닙니다")
    private String email;
}
