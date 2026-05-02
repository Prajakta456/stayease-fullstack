package com.codingshuttle.projects.airBnbApp.dto;

import com.codingshuttle.projects.airBnbApp.entity.enums.Role;
import lombok.Data;

import java.util.Set;

@Data
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private Set<Role> roles;
}
