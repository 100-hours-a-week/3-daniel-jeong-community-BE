package com.kakaotechbootcamp.community.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

/**
 * 로그인 요청 DTO
 */
@Getter
public class UserLoginRequestDto {
    
    @NotBlank(message = "이메일을 입력해주세요")
    @Email
	@Size(max = 320)
	@Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{1,}$", message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @Size(min = 8, max = 255)
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\[\\]{};':\\\"\\\\|,.<>/?-])(?!.*\\s)(?!.*(.)\\1{2,}).*$",
            message = "영문, 숫자, 특수문자가 필요합니다"
    )
    private String password;
}
