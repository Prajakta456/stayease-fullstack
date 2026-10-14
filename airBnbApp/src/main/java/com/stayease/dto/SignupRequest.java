package com.stayease.dto;

import com.stayease.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    // bcrypt only uses the first 72 bytes of a password, so longer ones are rejected rather than truncated
    @NotBlank
    @Size(min = 8, max = 72, message = "must be between 8 and 72 characters")
    private String password;

    private Role role; // GUEST (default) or HOTEL_MANAGER
}
