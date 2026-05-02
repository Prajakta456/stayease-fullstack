package com.codingshuttle.projects.airBnbApp.advice;

import com.codingshuttle.projects.airBnbApp.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(ResourceNotFoundException ex) {
        ApiError apiError = ApiError.builder().status(HttpStatus.NOT_FOUND).message(ex.getMessage()).build();
        return buildErrorResponseEntity(apiError);
    }

    // ✅ NEW: handle business logic errors (room unavailable, booking expired, etc.)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalState(IllegalStateException ex) {
        ApiError apiError = ApiError.builder().status(HttpStatus.CONFLICT).message(ex.getMessage()).build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgument(IllegalArgumentException ex) {
        ApiError apiError = ApiError.builder().status(HttpStatus.BAD_REQUEST).message(ex.getMessage()).build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleInternalServerError(Exception ex) {
        ApiError apiError = ApiError.builder().status(HttpStatus.INTERNAL_SERVER_ERROR).message(ex.getMessage()).build();
        return buildErrorResponseEntity(apiError);
    }

    private ResponseEntity<ApiResponse<?>> buildErrorResponseEntity(ApiError apiError) {
        return new ResponseEntity<>(new ApiResponse<>(apiError), apiError.getStatus());
    }
}
