package com.stayease.controller;

import com.stayease.dto.AuthResponse;
import com.stayease.dto.LoginRequest;
import com.stayease.dto.SignupRequest;
import com.stayease.security.RateLimiter;
import com.stayease.security.SecurityUtils;
import com.stayease.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Locale;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimiter rateLimiter;

    @Value("${security.rate-limit.login-per-ip-per-minute:10}")
    private int loginPerIpPerMinute;

    @Value("${security.rate-limit.login-per-email-per-minute:5}")
    private int loginPerEmailPerMinute;

    @Value("${security.rate-limit.signup-per-ip-per-hour:10}")
    private int signupPerIpPerHour;

    // NOTE: getRemoteAddr() is the proxy's address when running behind a load balancer;
    // configure server.forward-headers-strategy for that deployment instead of trusting X-Forwarded-For here.

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody @Valid SignupRequest request, HttpServletRequest http) {
        rateLimiter.consume("signup:ip:" + http.getRemoteAddr(), signupPerIpPerHour, Duration.ofHours(1));
        AuthResponse response = authService.signup(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request, HttpServletRequest http) {
        rateLimiter.consume("login:ip:" + http.getRemoteAddr(), loginPerIpPerMinute, Duration.ofMinutes(1));
        if (request.getEmail() != null) {
            rateLimiter.consume("login:email:" + request.getEmail().toLowerCase(Locale.ROOT),
                    loginPerEmailPerMinute, Duration.ofMinutes(1));
        }
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout(SecurityUtils.getCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
