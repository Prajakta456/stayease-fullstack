package com.stayease.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "a-signing-secret-that-is-at-least-32-bytes";

    @Test
    void requiredSecret_mustBeSet() {
        assertThatThrownBy(() -> new JwtService("", 60, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be set");
    }

    @Test
    void withoutTheRequirement_aRandomKeyIsUsedForDevelopment() {
        assertThatCode(() -> new JwtService("", 60, false)).doesNotThrowAnyException();
        assertThatCode(() -> new JwtService(SECRET, 60, true)).doesNotThrowAnyException();
    }

    @Test
    void shortSecrets_areRejected() {
        assertThatThrownBy(() -> new JwtService("too-short", 60, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
