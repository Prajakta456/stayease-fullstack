package com.codingshuttle.projects.airBnbApp.advice;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

/** Wraps every JSON response body in the {@link ApiResponse} envelope. */
@RestControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    // Tooling endpoints that must keep their own response format
    private static final List<String> EXCLUDED_PATHS = List.of("/v3/api-docs", "/actuator");

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Only JSON bodies: wrapping a String or byte[] body would fail in its non-JSON converter
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        String path = request.getURI().getPath();
        boolean isExcluded = EXCLUDED_PATHS.stream().anyMatch(path::contains);

        if (body instanceof ApiResponse<?> || isExcluded) {
            return body;
        }

        return new ApiResponse<>(body);
    }
}
