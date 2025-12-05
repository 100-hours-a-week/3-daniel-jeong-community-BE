package com.kakaotechbootcamp.community.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 정책 페이지 컨트롤러
 * JSON 파일에서 정책 내용을 읽어 Thymeleaf 템플릿에 전달합니다.
 */
@Controller
@RequestMapping("/policy")
@RequiredArgsConstructor
public class PolicyController {

    private static final String DATA_DIRECTORY = "data/";
    private static final String JSON_EXTENSION = ".json";
    private static final String TEMPLATE_NAME = "policy";
    private static final String REDIRECT_ROOT = "redirect:/";
    private static final String DATE_ATTRIBUTE = "date";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    private final ObjectMapper objectMapper;

    /**
     * 이용약관 페이지 렌더링
     */
    @GetMapping("/terms")
    public String terms(Model model) {
        return renderPolicyPage(PolicyType.TERMS, model);
    }

    /**
     * 개인정보처리방침 페이지 렌더링
     */
    @GetMapping("/privacy")
    public String privacy(Model model) {
        return renderPolicyPage(PolicyType.PRIVACY, model);
    }

    /**
     * 정책 페이지 렌더링
     */
    private String renderPolicyPage(PolicyType policyType, Model model) {
        try {
            Map<String, Object> policyData = loadPolicyData(policyType);
            if (policyData == null) {
                return REDIRECT_ROOT;
            }

            populateModel(model, policyData);
            return TEMPLATE_NAME;
        } catch (IOException e) {
            return REDIRECT_ROOT;
        }
    }

    /**
     * 정책 JSON 파일 로드 및 파싱
     */
    private Map<String, Object> loadPolicyData(PolicyType policyType) throws IOException {
        String resourcePath = DATA_DIRECTORY + policyType.getFileName() + JSON_EXTENSION;
        ClassPathResource resource = new ClassPathResource(resourcePath);

        if (!resource.exists()) {
            return null;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(
                inputStream,
                new TypeReference<Map<String, Object>>() {}
            );
        }
    }

    /**
     * 모델에 정책 데이터 설정
     * date 속성이 없으면 현재 날짜를 자동 추가합니다.
     */
    private void populateModel(Model model, Map<String, Object> policyData) {
        policyData.forEach(model::addAttribute);

        if (!model.containsAttribute(DATE_ATTRIBUTE)) {
            String currentDate = LocalDate.now().format(DATE_FORMATTER);
            model.addAttribute(DATE_ATTRIBUTE, currentDate);
        }
    }

    /**
     * 정책 타입 enum
     */
    private enum PolicyType {
        TERMS("terms"),
        PRIVACY("privacy");

        private final String fileName;

        PolicyType(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
