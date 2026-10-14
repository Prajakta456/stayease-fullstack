package com.codingshuttle.projects.airBnbApp.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingRequest {

    @NotNull
    private Long hotelId;

    @NotNull
    private Long roomId;

    @NotNull
    @FutureOrPresent(message = "must be today or later")
    private LocalDate checkInDate;

    @NotNull
    @Future(message = "must be after today")
    private LocalDate checkOutDate;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer roomsCount;
}
