package com.kakaotechbootcamp.community.dto.user;

import lombok.Getter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;

/**
 * 회원가입 요청 DTO
 * - 의도: 가입 시 필요한 사용자 입력 수집/검증
 */
@Getter
public class UserCreateRequestDto {

	@NotBlank
	@Email
	@Size(max = 320)
	@Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{1,}$", message = "올바른 이메일 형식이 아닙니다")
	private String email;

	@NotBlank
	@Size(min = 8, max = 255)
	@Pattern(
			regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=[\\]{};':\\\"\\\\|,.<>/?])(?!.*\\s)(?!.*(.)\\1{2,}).*$",
			message = "영문, 숫자, 특수문자가 필요합니다"
	)
	private String password;

	@NotBlank
	private String confirmPassword;
	
	@NotBlank
	@Size(min = 2, max = 10)
	@Pattern(
			regexp = "^(?!_)(?!.*__)(?!.*_$)(?!\\d+$)[가-힣a-zA-Z0-9_]+$",
			message = "한글, 영문, 숫자, 언더스코어(_)만 사용할 수 있습니다"
	)
	private String nickname;

    @Size(max = 1024)
    private String profileImageKey;

    /** 비밀번호 확인 검증 */
    @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치해야 합니다")
	public boolean isPasswordConfirmed() {
		return password != null && password.equals(confirmPassword);
	}
}
