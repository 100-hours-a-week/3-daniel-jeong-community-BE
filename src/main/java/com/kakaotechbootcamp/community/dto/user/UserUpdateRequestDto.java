package com.kakaotechbootcamp.community.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 사용자 정보 부분수정 요청 DTO
 * - 의도: 닉네임/프로필 등 선택적 변경 입력
 */
@Getter
public class UserUpdateRequestDto {

    @Size(min = 8, max = 255)
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=[\\]{};':\\\"\\\\|,.<>/?])(?!.*\\s)(?!.*(.)\\1{2,}).*$",
            message = "영문, 숫자, 특수문자가 필요합니다"
    )
    private String password;

    @Size(min = 2, max = 10)
    @Pattern(
            regexp = "^(?!_)(?!.*__)(?!.*_$)(?!\\d+$)[가-힣a-zA-Z0-9_]+$",
            message = "한글, 영문, 숫자, 언더스코어(_)만 사용할 수 있습니다"
    )
    private String nickname;

    @Size(max = 1024)
    private String profileImageKey;
}


