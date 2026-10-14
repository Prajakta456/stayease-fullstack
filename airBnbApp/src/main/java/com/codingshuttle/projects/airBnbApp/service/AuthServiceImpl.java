package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.exception.BadRequestException;
import com.codingshuttle.projects.airBnbApp.exception.ConflictException;
import com.codingshuttle.projects.airBnbApp.dto.AuthResponse;
import com.codingshuttle.projects.airBnbApp.dto.LoginRequest;
import com.codingshuttle.projects.airBnbApp.dto.SignupRequest;
import com.codingshuttle.projects.airBnbApp.entity.User;
import com.codingshuttle.projects.airBnbApp.entity.enums.Role;
import com.codingshuttle.projects.airBnbApp.repository.UserRepository;
import com.codingshuttle.projects.airBnbApp.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    /**
     * Roles a user may pick for themselves at signup. Anyone can become a host, like on Airbnb;
     * any future privileged role (e.g. ADMIN) must never be added here.
     */
    private static final Set<Role> SELF_ASSIGNABLE_ROLES = Set.of(Role.GUEST, Role.HOTEL_MANAGER);

    // bcrypt ignores everything after the first 72 bytes of a password
    private static final int MAX_PASSWORD_BYTES = 72;

    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final String EMAIL_TAKEN = "An account with this email already exists";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Compared against when the email is unknown, so a failed login costs the same bcrypt
    // work either way and response timing does not reveal which emails are registered
    private String dummyPasswordHash;

    @Override
    public AuthResponse signup(SignupRequest request) {
        Role role = (request.getRole() != null) ? request.getRole() : Role.GUEST;
        if (!SELF_ASSIGNABLE_ROLES.contains(role)) {
            throw new BadRequestException("This role cannot be selected at signup");
        }
        String email = normalizeEmail(request.getEmail());
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new BadRequestException("Password is too long");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(EMAIL_TAKEN);
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(role));

        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Another signup with the same email won the race past the existsByEmail check
            throw new ConflictException(EMAIL_TAKEN);
        }
        log.info("New user registered with id: {}", saved.getId());
        return toAuthResponse(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail())).orElse(null);
        if (user == null) {
            passwordEncoder.matches(String.valueOf(request.getPassword()), getDummyPasswordHash());
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
        if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
        log.info("User {} logged in", user.getId());
        return toAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(User user) {
        // Bumping the version revokes every token issued to this user so far
        userRepository.findById(user.getId()).ifPresent(u -> u.setTokenVersion(u.getTokenVersion() + 1));
        log.info("User {} logged out", user.getId());
    }

    // Emails are stored lowercase so "Asha@x.com" and "asha@x.com" are the same account
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(user.getId(), user.getName(), user.getEmail(), user.getRoles(), token);
    }

    private String getDummyPasswordHash() {
        if (dummyPasswordHash == null) {
            dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing-safety");
        }
        return dummyPasswordHash;
    }
}
