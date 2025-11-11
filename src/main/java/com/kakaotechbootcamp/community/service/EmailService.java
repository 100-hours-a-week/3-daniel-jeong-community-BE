package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.entity.User;
import com.kakaotechbootcamp.community.exception.BadRequestException;
import com.kakaotechbootcamp.community.exception.NotFoundException;
import com.kakaotechbootcamp.community.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import com.kakaotechbootcamp.community.config.EmailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final TemplateEngine templateEngine;
    private final PasswordEncoder passwordEncoder;
    private final EmailProperties emailProperties;

    private record VerificationCodeInfo(String code, long expiresAt) {}

    private final Map<String, Long> emailLastSentTime = new ConcurrentHashMap<>();
    private final Map<String, VerificationCodeInfo> verificationCodes = new ConcurrentHashMap<>();
    private final Map<String, Boolean> verifiedEmails = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    public Integer sendPasswordResetCode(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("등록된 이메일이 아닙니다"));

        long currentTime = System.currentTimeMillis();
        Long lastSentTime = emailLastSentTime.get(email);
        if (lastSentTime != null && (currentTime - lastSentTime) < emailProperties.getMinIntervalMs()) {
            long remainingMs = emailProperties.getMinIntervalMs() - (currentTime - lastSentTime);
            String remainingTime = formatRemainingTime(remainingMs);
            long intervalMinutes = emailProperties.getMinIntervalMs() / 60000;
            throw new BadRequestException("이메일 발송은 " + intervalMinutes + "분에 1회만 가능합니다. 남은 시간: " + remainingTime);
        }

        String code = generateVerificationCode();
        verificationCodes.put(email, new VerificationCodeInfo(code, currentTime + (emailProperties.getCodeExpirationMinutes() * 60L * 1000)));
        verifiedEmails.remove(email); // 재발송 시 이전 검증 상태 초기화
        emailLastSentTime.put(email, currentTime);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(email);
            helper.setSubject("[아무말대잔치] 비밀번호 재설정 인증번호 안내");
            
            Context context = new Context();
            context.setVariable("code", code);
            context.setVariable("expirationMinutes", emailProperties.getCodeExpirationMinutes());
            
            String htmlContent = templateEngine.process("password-reset", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다", e);
        }

        return user.getId();
    }

    public void verifyCode(String email, String code) {
        VerificationCodeInfo codeInfo = verificationCodes.get(email);
        if (codeInfo == null) {
            throw new BadRequestException("인증번호가 발송되지 않았거나 만료되었습니다");
        }
        if (System.currentTimeMillis() > codeInfo.expiresAt()) {
            verificationCodes.remove(email);
            verifiedEmails.remove(email);
            throw new BadRequestException("인증번호가 만료되었습니다. 다시 요청해주세요");
        }
        if (!codeInfo.code().equals(code.trim())) {
            throw new BadRequestException("인증번호가 올바르지 않습니다");
        }
        // 검증 완료 상태 저장
        verifiedEmails.put(email, true);
    }

    public boolean isVerified(String email) {
        return Boolean.TRUE.equals(verifiedEmails.get(email));
    }

    public void removeCode(String email) {
        verificationCodes.remove(email);
        verifiedEmails.remove(email);
    }

    /**
     * 비밀번호 재설정 인증번호 검증 (코드 유지)
     * - 의도: 인증번호만 검증하여 UI 활성화
     */
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> verifyPasswordResetCode(Integer id, String verificationCode) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        String code = verificationCode == null ? "" : verificationCode.trim();
        if (code.isEmpty()) {
            throw new BadRequestException("인증번호를 입력해주세요");
        }

        String email = user.getEmail().toLowerCase();
        // 인증번호 검증
        verifyCode(email, code);

        Map<String, Object> result = new HashMap<>();
        result.put("verified", true);
        return ApiResponse.modified(result);
    }

    /**
     * 비밀번호 재설정
     * - 인증번호 검증 완료 후 새 비밀번호 + 확인 비밀번호 검증 후 변경
     */
    @Transactional
    public ApiResponse<Void> resetPassword(Integer id, String newPassword, String confirmPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        String email = user.getEmail().toLowerCase();
        // 인증번호 검증 완료 여부 확인
        if (!isVerified(email)) {
            throw new BadRequestException("인증번호를 먼저 검증해주세요");
        }

        String next = newPassword == null ? "" : newPassword.trim();
        if (next.isEmpty()) {
            throw new BadRequestException("새 비밀번호를 입력해주세요");
        }

        String confirm = confirmPassword == null ? "" : confirmPassword.trim();
        if (confirm.isEmpty()) {
            throw new BadRequestException("비밀번호 확인을 입력해주세요");
        }

        if (!next.equals(confirm)) {
            throw new BadRequestException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다");
        }

        // 비밀번호 재설정 및 코드 삭제
        String encodedNewPassword = passwordEncoder.encode(next);
        user.updatePassword(encodedNewPassword);
        removeCode(email);

        return ApiResponse.modified(null);
    }

    private String generateVerificationCode() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }

    private String formatRemainingTime(long remainingMs) {
        long totalSeconds = remainingMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes > 0 ? minutes + "분 " + seconds + "초" : seconds + "초";
    }
}