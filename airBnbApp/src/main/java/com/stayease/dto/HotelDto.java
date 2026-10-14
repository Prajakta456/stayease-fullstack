package com.stayease.dto;

import com.stayease.entity.HotelContactInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class HotelDto {
    private Long id;

    // Required on create; on update, omitted fields keep their current value
    @NotBlank(groups = OnCreate.class)
    @Size(min = 1, max = 200)
    private String name;

    @NotBlank(groups = OnCreate.class)
    @Size(min = 1, max = 100)
    private String city;

    @Size(max = 20)
    private List<@NotBlank @Size(max = 255) String> photos;

    @Size(max = 30)
    private List<@NotBlank @Size(max = 100) String> amenities;

    @Valid
    private HotelContactInfo contactInfo;

    // Read-only: publishing is done with PATCH /admin/hotels/{id}/activate and /deactivate
    private Boolean active;
}
