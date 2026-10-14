package com.stayease.dto;

import com.stayease.entity.enums.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GuestDto {
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String name;

    private Gender gender;

    @NotNull
    @Min(0)
    @Max(120)
    private Integer age;
}
