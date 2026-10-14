package com.codingshuttle.projects.airBnbApp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class HotelSearchRequest {

    // A city or part of a hotel name; blank means anywhere
    @Size(max = 100)
    private String city;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer roomsCount;

    // Out-of-range values are clamped by the service (page >= 0, 1 <= size <= 50)
    private Integer page = 0;
    private Integer size = 10;
}
