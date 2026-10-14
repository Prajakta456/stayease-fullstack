package com.stayease.controller;

import com.stayease.dto.BookingDto;
import com.stayease.dto.BookingRequest;
import com.stayease.dto.GuestDto;
import com.stayease.service.BookingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookings")
public class HotelBookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<List<BookingDto>> getMyBookings() {
        return ResponseEntity.ok(bookingService.getMyBookings());
    }

    @PostMapping("/init")
    public ResponseEntity<BookingDto> initialiseBooking(@RequestBody @Valid BookingRequest bookingRequest) {
        return ResponseEntity.ok(bookingService.initialiseBooking(bookingRequest));
    }

    @PostMapping("/{bookingId}/addGuests")
    public ResponseEntity<BookingDto> addGuests(@PathVariable Long bookingId,
                                                @RequestBody @NotEmpty(message = "at least one guest is required")
                                                List<@Valid GuestDto> guestDtoList) {
        return ResponseEntity.ok(bookingService.addGuests(bookingId, guestDtoList));
    }

    // Charges the guest through the configured PaymentGateway (a mock by default) and confirms the booking
    @PostMapping("/{bookingId}/payments")
    public ResponseEntity<BookingDto> payForBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.payForBooking(bookingId));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingDto> cancelBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }
}
