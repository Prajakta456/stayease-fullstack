package com.stayease.advice;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Replaces Spring Boot's default /error endpoint, which handles failures that happen outside a
 * controller (e.g. in a servlet filter). It answers in the same {@link ApiResponse} envelope as
 * every other endpoint, with no stack traces or exception messages.
 */
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<ApiResponse<?>> handleError(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = code instanceof Integer value && HttpStatus.resolve(value) != null
                ? HttpStatus.resolve(value)
                : HttpStatus.INTERNAL_SERVER_ERROR;

        String message = switch (status) {
            case NOT_FOUND -> "Resource not found";
            default -> status.is5xxServerError() ? GlobalExceptionHandler.UNEXPECTED_ERROR : status.getReasonPhrase();
        };
        ApiError apiError = ApiError.builder().status(status).message(message).build();
        return ResponseEntity.status(status).body(new ApiResponse<>(apiError));
    }
}
