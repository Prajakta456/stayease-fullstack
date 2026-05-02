package com.codingshuttle.projects.airBnbApp.dto;

import com.codingshuttle.projects.airBnbApp.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class AuthResponse {
    private Long id;
    private String name;
    private String email;
    private Set<Role> roles;
    private String token; // Simple session token (userId encoded as base64 for this demo)
}
