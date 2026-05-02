package com.codingshuttle.projects.airBnbApp.controller;

import com.codingshuttle.projects.airBnbApp.dto.AuthResponse;
import com.codingshuttle.projects.airBnbApp.dto.LoginRequest;
import com.codingshuttle.projects.airBnbApp.dto.SignupRequest;
import com.codingshuttle.projects.airBnbApp.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        log.info("Signup attempt for email: {}", request.getEmail());
        AuthResponse response = authService.signup(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
