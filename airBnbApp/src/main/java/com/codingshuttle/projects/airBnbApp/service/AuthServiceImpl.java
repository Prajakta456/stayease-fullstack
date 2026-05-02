package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.dto.AuthResponse;
import com.codingshuttle.projects.airBnbApp.dto.LoginRequest;
import com.codingshuttle.projects.airBnbApp.dto.SignupRequest;
import com.codingshuttle.projects.airBnbApp.entity.User;
import com.codingshuttle.projects.airBnbApp.entity.enums.Role;
import com.codingshuttle.projects.airBnbApp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Default role is GUEST if not specified
        Role role = (request.getRole() != null) ? request.getRole() : Role.GUEST;
        user.setRoles(Set.of(role));

        User saved = userRepository.save(user);
        String token = generateToken(saved);

        return new AuthResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getRoles(), token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = generateToken(user);
        return new AuthResponse(user.getId(), user.getName(), user.getEmail(), user.getRoles(), token);
    }

    // Simple token: base64(userId:email) — for a real app, use JWT
    private String generateToken(User user) {
        String raw = user.getId() + ":" + user.getEmail() + ":" + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }
}