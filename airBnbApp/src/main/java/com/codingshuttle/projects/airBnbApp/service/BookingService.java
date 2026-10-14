package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.dto.BookingDto;
import com.codingshuttle.projects.airBnbApp.dto.BookingRequest;
import com.codingshuttle.projects.airBnbApp.dto.GuestDto;

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
