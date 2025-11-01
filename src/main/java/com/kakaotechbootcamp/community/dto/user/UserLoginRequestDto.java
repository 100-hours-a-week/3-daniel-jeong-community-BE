package com.kakaotechbootcamp.community.dto.user;

import lombok.Getter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * 로그인 요청 DTO
 * - 의도: 이메일/비밀번호로 로그인
 */
@Getter
public class UserLoginRequestDto {

    @NotBlank
    @Email
    @Size(max = 320)
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{1,}$", message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank
    @Size(min = 8, max = 255)
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\[\\]{};':\\\"\\\\|,.<>/?-])(?!.*\\s)(?!.*(.)\\1{2,}).*$",
        message = "영문, 숫자, 특수문자가 필요합니다"
    )
    private String password;
}
