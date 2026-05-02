package com.codingshuttle.projects.airBnbApp.config;

import com.codingshuttle.projects.airBnbApp.entity.User;
import com.codingshuttle.projects.airBnbApp.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TokenAuthFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                // Token format: base64(userId:email:timestamp)
                String decoded = new String(Base64.getDecoder().decode(token));
                String[] parts = decoded.split(":");
                if (parts.length >= 2) {
                    String email = parts[1];
                    User user = userRepository.findByEmail(email).orElse(null);
                    if (user != null) {
                        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                                .collect(Collectors.toList());
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(email, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception ignored) {
                // Invalid token — continue unauthenticated
            }
        }
        filterChain.doFilter(request, response);
    }
}
