package com.codingshuttle.projects.airBnbApp.advice;

import com.codingshuttle.projects.airBnbApp.exception.BadRequestException;
import com.codingshuttle.projects.airBnbApp.exception.ConflictException;
import com.codingshuttle.projects.airBnbApp.exception.ResourceNotFoundException;
import com.codingshuttle.projects.airBnbApp.exception.TooManyRequestsException;
import com.codingshuttle.projects.airBnbApp.service.payment.PaymentFailedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns every error into the {@link ApiResponse} envelope with the right HTTP status.
 * Only messages written by this application reach the client: framework and library
 * exception messages can contain SQL, class names or internal state, so they are logged
 * server-side and replaced by a safe description.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String UNEXPECTED_ERROR = "An unexpected error occurred";

    // ---------- application exceptions (their messages are written for the client) ----------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildErrorResponseEntity(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequest(BadRequestException ex) {
        return buildErrorResponseEntity(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Business-rule violations (room unavailable, booking expired, email taken, ...)
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<?>> handleConflict(ConflictException ex) {
        return buildErrorResponseEntity(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ApiResponse<?>> handlePaymentFailed(PaymentFailedException ex) {
        return buildErrorResponseEntity(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiResponse<?>> handleTooManyRequests(TooManyRequestsException ex) {
        ApiError apiError = ApiError.builder().status(HttpStatus.TOO_MANY_REQUESTS).message(ex.getMessage()).build();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(new ApiResponse<>(apiError));
    }

    // ---------- security ----------

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handleBadCredentials(BadCredentialsException ex) {
        return buildErrorResponseEntity(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthentication(AuthenticationException ex) {
        return buildErrorResponseEntity(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponseEntity(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }

    // ---------- anything else: log the details, never send them ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleInternalServerError(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildErrorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR);
    }

    // ---------- Spring MVC request errors ----------

    // @Valid request body failed Bean Validation
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        return validationErrorResponse(ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList());
    }

    // Constraints on controller method parameters (e.g. List<@Valid GuestDto>) failed
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers, HttpStatusCode status,
                                                                            WebRequest request) {
        return validationErrorResponse(ex.getAllErrors().stream()
                .map(error -> error instanceof FieldError fieldError
                        ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                        : error.getDefaultMessage())
                .toList());
    }

    // Unparseable JSON, or a value of the wrong type (e.g. "checkInDate": "tomorrow", "role": "ADMIN")
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        String message = "Request body is missing or malformed";
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            message = "Invalid value for field '" + fieldPath(mismatch) + "'";
        } else if (ex.getCause() instanceof JsonProcessingException) {
            message = "Malformed JSON in request body";
        }
        return errorResponse(HttpStatus.BAD_REQUEST, message);
    }

    // A path variable or query parameter of the wrong type, e.g. /hotels/abc/info
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName() : ex.getPropertyName();
        return errorResponse(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + name + "'");
    }

    // Every other Spring MVC exception (404, 405, 415, missing parameter, ...) ends up here
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null || status.is5xxServerError()) {
            log.error("Request failed with status {}", statusCode.value(), ex);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR);
        }
        return ResponseEntity.status(status).headers(headers).body(new ApiResponse<>(
                ApiError.builder().status(status).message(clientMessage(ex, status)).build()));
    }

    private static String clientMessage(Exception ex, HttpStatus status) {
        if (ex instanceof NoResourceFoundException) {
            return "Resource not found";
        }
        if (ex instanceof HttpRequestMethodNotSupportedException e) {
            return "Method " + e.getMethod() + " is not supported for this endpoint";
        }
        if (ex instanceof HttpMediaTypeNotSupportedException) {
            return "Unsupported content type; send application/json";
        }
        if (ex instanceof HttpMediaTypeNotAcceptableException) {
            return "Requested response type is not supported";
        }
        if (ex instanceof MissingServletRequestParameterException e) {
            return "Missing required parameter '" + e.getParameterName() + "'";
        }
        return status.getReasonPhrase();
    }

    private static String fieldPath(JsonMappingException ex) {
        return ex.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining(".")).replace(".[", "[");
    }

    private static ResponseEntity<Object> validationErrorResponse(List<String> subErrors) {
        ApiError apiError = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("Invalid request: " + String.join("; ", subErrors))
                .subErrors(subErrors)
                .build();
        return ResponseEntity.badRequest().body(new ApiResponse<>(apiError));
    }

    private static ResponseEntity<Object> errorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiResponse<>(ApiError.builder().status(status).message(message).build()));
    }

    private static ResponseEntity<ApiResponse<?>> buildErrorResponseEntity(HttpStatus status, String message) {
        ApiError apiError = ApiError.builder().status(status).message(message).build();
        return new ResponseEntity<>(new ApiResponse<>(apiError), status);
    }
}
