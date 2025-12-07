package com.kakaotechbootcamp.community.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import com.kakaotechbootcamp.community.exception.BadRequestException;
import com.kakaotechbootcamp.community.exception.ConflictException;
import com.kakaotechbootcamp.community.exception.NotFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400: 입력값 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleValidationException(MethodArgumentNotValidException e) {
        List<String> errors = e.getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.badRequest(errors));
    }

    // 400: 잘못된 요청 (Bad Request)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<String>> handleBadRequestException(BadRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.badRequest(e.getMessage()));
    }
        
    // 401: 인증 실패
    @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
    public ResponseEntity<ApiResponse<String>> handleUnauthorizedException(HttpClientErrorException.Unauthorized e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.unauthorized(e.getMessage()));
    }

    // 403: 권한 없음
    @ExceptionHandler(HttpClientErrorException.Forbidden.class)
    public ResponseEntity<ApiResponse<String>> handleForbiddenException(HttpClientErrorException.Forbidden e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.forbidden(e.getMessage()));
    }


    // 404: 도메인 NotFound
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNotFoundException(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound(e.getMessage()));
    }

    // 409: 중복 충돌
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<String>> handleConflictException(ConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.conflict(e.getMessage()));
    }
}
