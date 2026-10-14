package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.dto.HotelPriceDto;
import com.codingshuttle.projects.airBnbApp.dto.HotelSearchRequest;
import com.codingshuttle.projects.airBnbApp.dto.RoomDto;
import com.codingshuttle.projects.airBnbApp.entity.Room;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface InventoryService {

    /** Creates any missing nightly inventory rows for the room from today up to the booking horizon (1 year). */
    void ensureInventory(Room room);

    /** Same as {@link #ensureInventory(Room)}, by id; used by the daily job that rolls the horizon forward. */
    void ensureInventory(Long roomId);

    void deleteAllInventories(Room room);

    Page<HotelPriceDto> searchHotels(HotelSearchRequest hotelSearchRequest);

    /** Fills {@link RoomDto#getPrice()} and {@link RoomDto#getAvailable()} for a stay at the given hotel. */
    void applyStayPricing(Long hotelId, List<RoomDto> rooms, LocalDate checkIn, LocalDate checkOut, int roomsCount);
}
