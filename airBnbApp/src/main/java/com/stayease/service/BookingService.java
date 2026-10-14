package com.stayease.service;

import com.stayease.dto.BookingDto;
import com.stayease.dto.BookingRequest;
import com.stayease.dto.GuestDto;

import java.util.List;

public interface BookingService {

    BookingDto initialiseBooking(BookingRequest bookingRequest);

    BookingDto addGuests(Long bookingId, List<GuestDto> guestDtoList);

    BookingDto payForBooking(Long bookingId);

    BookingDto cancelBooking(Long bookingId);

    List<BookingDto> getMyBookings();

    /** Expires the booking if its hold ran out unpaid, releasing its rooms. @return true if expired */
    boolean expireIfHoldElapsed(Long bookingId);
}
