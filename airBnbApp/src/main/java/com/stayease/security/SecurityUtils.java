package com.stayease.security;

import com.stayease.entity.User;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * The user authenticated by {@link JwtAuthFilter} for the current request.
     *
     * @throws AuthenticationCredentialsNotFoundException if the request is anonymous
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return user;
    }
}
