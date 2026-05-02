package com.codingshuttle.projects.airBnbApp.dto;

import com.codingshuttle.projects.airBnbApp.entity.enums.Gender;
import lombok.Data;

@Data
public class GuestDto {
    private Long id;
    // ✅ FIXED: removed `User user` field — was leaking full User entity (password hash etc.) in API responses
    private String name;
    private Gender gender;
    private Integer age;
}
