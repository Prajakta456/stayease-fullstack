package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.dto.AuthResponse;
import com.codingshuttle.projects.airBnbApp.dto.LoginRequest;
import com.codingshuttle.projects.airBnbApp.dto.SignupRequest;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
}
