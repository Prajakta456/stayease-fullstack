package com.codingshuttle.projects.airBnbApp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RoomDto {
    private Long id;

    @NotBlank(groups = OnCreate.class)
    @Size(min = 1, max = 100)
    private String type;

    @NotNull(groups = OnCreate.class)
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal basePrice;

    @Size(max = 20)
    private List<@NotBlank @Size(max = 255) String> photos;

    @Size(max = 30)
    private List<@NotBlank @Size(max = 100) String> amenities;

    // Number of rooms of this type
    @NotNull(groups = OnCreate.class)
    @Min(1)
    @Max(1000)
    private Integer totalCount;

    // Guests per room
    @NotNull(groups = OnCreate.class)
    @Min(1)
    @Max(20)
    private Integer capacity;

    // Only filled in by GET /hotels/{id}/info when stay dates are given:
    // the average nightly price for those dates, and whether the room can be booked for all of them
    private BigDecimal price;
    private Boolean available;
}
