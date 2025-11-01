package com.kakaotechbootcamp.community.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 정책 페이지 컨트롤러
 * - 이용약관, 개인정보처리방침 등 정책 페이지 렌더링
 * - JSON 파일에서 정책 내용을 읽어서 Thymeleaf 템플릿에 전달
 * - @Controller: Thymeleaf 템플릿 렌더링용
 * 
 * JSON 파일 구조:
 * - 파일 위치: classpath:policy/{type}.json
 * - 필수 필드: title (제목)
 * - 선택 필드: subtitle (부제목), date (시행일), sections (섹션 배열)
 * - 섹션 구조: { title, paragraphs[], lists[] }
 * - 연락처: contactTitle, contactInfo
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PolicyController {

    private final ObjectMapper objectMapper;

    /**
     * 이용약관 페이지
     * - 경로: GET /terms
     * - 파일: classpath:policy/terms.json
     * - 템플릿: policy.html
     */
    @GetMapping("/terms")
    public String terms(Model model) {
        return loadPolicy("terms", model);
    }

    /**
     * 개인정보처리방침 페이지
     * - 경로: GET /privacy
     * - 파일: classpath:policy/privacy.json
     * - 템플릿: policy.html
     */
    @GetMapping("/privacy")
    public String privacy(Model model) {
        return loadPolicy("privacy", model);
    }

    /**
     * JSON 파일에서 정책 내용 로드 및 모델 설정
     * - 파일 경로: classpath:policy/{type}.json
     * - date가 없으면 현재 날짜 자동 추가
     * - 파일 없음/읽기 실패 시 루트로 리다이렉트
     */
    private String loadPolicy(String type, Model model) {
        try {
            ClassPathResource resource = new ClassPathResource("policy/" + type + ".json");
            
            if (!resource.exists()) {
                log.warn("정책 파일을 찾을 수 없습니다: {}", resource.getPath());
                return "redirect:/";
            }

            try (InputStream inputStream = resource.getInputStream()) {
                // JSON을 Map<String, Object>로 파싱
                Map<String, Object> policyData = objectMapper.readValue(
                    inputStream,
                    new TypeReference<Map<String, Object>>() {}
                );

                // JSON의 모든 키-값을 모델 속성으로 추가
                // 예: title, subtitle, date, sections, contactTitle, contactInfo
                policyData.forEach(model::addAttribute);

                // date가 없으면 현재 날짜 자동 추가
                if (!model.containsAttribute("date")) {
                    model.addAttribute("date", 
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
                }

                return "policy";
            }
        } catch (IOException e) {
            log.error("정책 파일 읽기 실패: {}", type, e);
            return "redirect:/";
        }
    }
}
