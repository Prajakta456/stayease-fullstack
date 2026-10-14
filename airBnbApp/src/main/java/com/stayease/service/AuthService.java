package com.stayease.service;

import com.stayease.dto.AuthResponse;
import com.stayease.dto.LoginRequest;
import com.stayease.dto.SignupRequest;
import com.stayease.entity.User;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
    void logout(User user);
}
