package com.codingshuttle.projects.airBnbApp.dto;

import com.codingshuttle.projects.airBnbApp.entity.enums.Role;
import lombok.Data;

@Data
public class SignupRequest {
    private String name;
    private String email;
    private String password;
    private Role role; // GUEST or HOTEL_MANAGER
}
