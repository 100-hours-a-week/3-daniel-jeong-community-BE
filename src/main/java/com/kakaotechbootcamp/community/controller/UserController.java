package com.kakaotechbootcamp.community.controller;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.common.ImageProperties;
import com.kakaotechbootcamp.community.dto.user.UserCreateRequestDto;
import com.kakaotechbootcamp.community.dto.user.UserResponseDto;
import com.kakaotechbootcamp.community.dto.user.UserUpdateRequestDto;
import com.kakaotechbootcamp.community.exception.BadRequestException;
import com.kakaotechbootcamp.community.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 사용자(User) API 컨트롤러
 * - 회원가입, 조회, 부분수정, 탈퇴
 * @RestController: @Controller + @ResponseBody
 * @RequestMapping: 기본 경로 /users
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final ImageProperties imageProperties;

    /**
     * 이메일 중복 체크
     * - 반환: true=사용 가능, false=중복
     */
    @PostMapping("/check-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkEmail(@RequestBody Map<String, String> body) {
        ApiResponse<Map<String, Object>> response = userService.isEmailAvailable(body.get("email"));
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 닉네임 중복 체크
     * - 반환: true=사용 가능, false=중복
     */
    @PostMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Boolean>> checkNickname(@RequestBody Map<String, String> body) {
        ApiResponse<Boolean> response = userService.isNicknameAvailable(body.get("nickname"));
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 회원가입
     * - 의도: 유효성 검증(@Valid) 후 사용자 생성
     * - 요청: multipart/form-data (userData: JSON, profileImage: 파일, 선택사항)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponseDto>> create(
            @RequestPart("userData") @Valid UserCreateRequestDto request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        // 프로필 이미지 파일 검증 (있는 경우)
        if (profileImage != null && !profileImage.isEmpty()) {
            if (profileImage.getSize() > imageProperties.getMaxSizeBytes()) {
                throw new BadRequestException("이미지 최대 크기" + imageProperties.getMaxSizeBytes() + "바이트를 초과했습니다");
            }
            if (!imageProperties.isAllowedContentType(profileImage.getContentType())) {
                throw new BadRequestException("지원하지 않는 이미지 확장자입니다: " + profileImage.getContentType());
            }
        }
        
        ApiResponse<UserResponseDto> response = userService.create(request, profileImage);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

    /**
     * 회원 단건 조회
     * - 의도: id로 활성 사용자 조회, 없으면 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getById(@PathVariable Integer id) {
        ApiResponse<UserResponseDto> response = userService.getById(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

    /**
     * 회원 정보 부분 수정
     * - 의도: 닉네임/프로필 선택 수정, 닉네임 중복 시 409
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> update(@PathVariable Integer id, @Valid @RequestBody UserUpdateRequestDto request) {
        ApiResponse<UserResponseDto> response = userService.update(id, request);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

    /**
     * 회원 탈퇴(소프트 삭제)
     * - 의도: deletedAt 설정으로 비활성화
     */
    @DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
		ApiResponse<Void> response = userService.delete(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

    /**
     * 비밀번호 변경
     * - 의도: 현재/신규 비밀번호 검증 후 변경(공백/동일/불일치 시 400)
     */
    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        ApiResponse<Void> response = userService.updatePassword(id, body.get("currentPassword"), body.get("newPassword"));
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
