package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.dto.HotelDto;
import com.codingshuttle.projects.airBnbApp.dto.HotelInfoDto;

import java.time.LocalDate;
import java.util.List;

public interface HotelService {
    HotelDto createNewHotel(HotelDto hotelDto);

    List<HotelDto> getAllHotels();

    HotelDto getHotelById(Long id);

    HotelDto updateHotelById(Long id, HotelDto hotelDto);

    void deleteHotelById(Long id);

    void activateHotel(Long hotelId);

    void deactivateHotel(Long hotelId);

    /** Public hotel page. With stay dates, each room also gets its price and availability for that stay. */
    HotelInfoDto getHotelInfoById(Long hotelId, LocalDate checkIn, LocalDate checkOut, Integer roomsCount);
}
