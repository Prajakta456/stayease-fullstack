package com.stayease.security;

import com.stayease.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32; // HS256 requires a 256-bit key
    static final String CLAIM_TOKEN_VERSION = "ver";

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(@Value("${jwt.secret:}") String secret,
                      @Value("${jwt.expiration-minutes:60}") long expirationMinutes) {
        this.key = buildKey(secret);
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String generateToken(User user) {
        return generateToken(user, expiration);
    }

    String generateToken(User user, Duration validFor) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_TOKEN_VERSION, user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validFor)))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies signature and expiry.
     *
     * @throws JwtException if the token is malformed, tampered with or expired
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Duration getExpiration() {
        return expiration;
    }

    private static SecretKey buildKey(String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("jwt.secret / JWT_SECRET is not set — using a random signing key. "
                    + "Tokens will not survive a restart. Set JWT_SECRET in production.");
            byte[] random = new byte[MIN_SECRET_BYTES];
            new SecureRandom().nextBytes(random);
            return Keys.hmacShaKeyFor(random);
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes long");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
