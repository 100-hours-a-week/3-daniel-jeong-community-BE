package com.kakaotechbootcamp.community.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private final boolean success;
    private final int status;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, HttpStatus.OK.value(), ApiMessage.SUCCESS.getMessage(), null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, HttpStatus.CREATED.value(), ApiMessage.CREATED.getMessage(), data);
    }

    public static <T> ApiResponse<T> modified(T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), ApiMessage.MODIFIED.getMessage(), data);
    }

    public static <T> ApiResponse<T> deleted(T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), ApiMessage.DELETED.getMessage(), data);
    }
    public static <T> ApiResponse<T> badRequest(T data) {
        return new ApiResponse<>(false, HttpStatus.BAD_REQUEST.value(), ApiMessage.BAD_REQUEST.getMessage(), data);
    }

    public static <T> ApiResponse<T> unauthorized(T data) {
        return new ApiResponse<>(false, HttpStatus.UNAUTHORIZED.value(), ApiMessage.UNAUTHORIZED.getMessage(), data);
    }

    public static <T> ApiResponse<T> forbidden(T data) {
        return new ApiResponse<>(false, HttpStatus.FORBIDDEN.value(), ApiMessage.FORBIDDEN.getMessage(), data);
    }

    public static <T> ApiResponse<T> notFound(T data) {
        return new ApiResponse<>(false, HttpStatus.NOT_FOUND.value(), ApiMessage.NOT_FOUND.getMessage(), data);
    }

    public static <T> ApiResponse<T> conflict(T data) {
        return new ApiResponse<>(false, HttpStatus.CONFLICT.value(), ApiMessage.CONFLICT.getMessage(), data);
    }
}
