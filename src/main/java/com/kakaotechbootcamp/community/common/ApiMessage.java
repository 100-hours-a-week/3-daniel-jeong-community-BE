package com.kakaotechbootcamp.community.common;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum ApiMessage {
    // 2xx Success
    SUCCESS("Success", HttpStatus.OK),
    CREATED("Created", HttpStatus.CREATED),
    MODIFIED("Modified", HttpStatus.OK),
    DELETED("Deleted", HttpStatus.OK),

    // 4xx Client Errors
    BAD_REQUEST("Bad Request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("Forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND("Not Found", HttpStatus.NOT_FOUND),
    CONFLICT("Conflict", HttpStatus.CONFLICT),

    // 5xx Server Errors
    INTERNAL_SERVER_ERROR("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus httpStatus;
}
