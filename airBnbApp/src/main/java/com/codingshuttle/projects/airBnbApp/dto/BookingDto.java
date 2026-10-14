package com.codingshuttle.projects.airBnbApp.dto;

import com.codingshuttle.projects.airBnbApp.entity.enums.BookingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class BookingDto {
    private Long id;
    private Long hotelId;
    private String hotelName;
    private String hotelCity;
    private Long roomId;
    private String roomType;
    private Integer roomsCount;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Until this moment the rooms are held for the guest; unpaid bookings then expire
    private LocalDateTime holdExpiresAt;
    private BookingStatus bookingStatus;
    // Total price the guest is charged, computed server-side from the nightly inventory prices
    private BigDecimal amount;
    private Set<GuestDto> guests;
}
