package com.stayease.service;

import com.stayease.exception.ConflictException;
import com.stayease.dto.HotelDto;
import com.stayease.dto.HotelInfoDto;
import com.stayease.dto.RoomDto;
import com.stayease.entity.Hotel;
import com.stayease.entity.Room;
import com.stayease.exception.ResourceNotFoundException;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.HotelRepository;
import com.stayease.repository.InventoryRepository;
import com.stayease.repository.RoomRepository;
import com.stayease.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final BookingRepository bookingRepository;

    @Override
    public HotelDto createNewHotel(HotelDto hotelDto) {
        log.info("Creating a new hotel with name: {}", hotelDto.getName());
        Hotel hotel = modelMapper.map(hotelDto, Hotel.class);
        hotel.setId(null);
        // New hotels start inactive; PATCH /activate publishes them once rooms are added
        hotel.setActive(false);
        hotel.setOwner(SecurityUtils.getCurrentUser());
        hotel = hotelRepository.save(hotel);
        log.info("Created a new hotel with ID: {}", hotel.getId());
        return modelMapper.map(hotel, HotelDto.class);
    }

    // Only the hotels owned by the signed-in manager
    @Override
    @Transactional(readOnly = true)
    public List<HotelDto> getAllHotels() {
        log.info("Fetching all hotels of the current manager");
        return hotelRepository.findAllByOwnerId(SecurityUtils.getCurrentUser().getId())
                .stream()
                .map(hotel -> modelMapper.map(hotel, HotelDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public HotelDto getHotelById(Long id) {
        log.info("Getting the hotel with ID: {}", id);
        return modelMapper.map(getOwnedHotel(id), HotelDto.class);
    }

    @Override
    public HotelDto updateHotelById(Long id, HotelDto hotelDto) {
        log.info("Updating the hotel with ID: {}", id);
        Hotel hotel = getOwnedHotel(id);
        String previousCity = hotel.getCity();
        Boolean active = hotel.getActive();

        // Omitted (null) fields keep their current value
        modelMapper.map(hotelDto, hotel);
        hotel.setId(id);
        // Publishing is only done through activate/deactivate, which also manage inventory
        hotel.setActive(active);

        if (!Objects.equals(previousCity, hotel.getCity())) {
            inventoryRepository.updateCityForHotel(id, hotel.getCity());
        }
        hotel = hotelRepository.save(hotel);
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    public void deleteHotelById(Long id) {
        Hotel hotel = getOwnedHotel(id);
        if (bookingRepository.existsByHotelId(id)) {
            throw new ConflictException("This hotel has bookings and cannot be deleted; deactivate it instead");
        }

        for (Room room : hotel.getRooms()) {
            inventoryService.deleteAllInventories(room);
            roomRepository.deleteById(room.getId());
        }
        hotelRepository.deleteById(id);
    }

    @Override
    public void activateHotel(Long hotelId) {
        log.info("Activating the hotel with ID: {}", hotelId);
        Hotel hotel = getOwnedHotel(hotelId);

        hotel.setActive(true);

        // Idempotent: only nights that don't exist yet are created
        for (Room room : hotel.getRooms()) {
            inventoryService.ensureInventory(room);
        }
    }

    @Override
    public void deactivateHotel(Long hotelId) {
        log.info("Deactivating the hotel with ID: {}", hotelId);
        // Hidden from search and closed to new bookings; existing bookings are kept
        getOwnedHotel(hotelId).setActive(false);
    }

    @Override
    @Transactional(readOnly = true)
    public HotelInfoDto getHotelInfoById(Long hotelId, LocalDate checkIn, LocalDate checkOut, Integer roomsCount) {
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .filter(h -> Boolean.TRUE.equals(h.getActive())) // unpublished hotels are not public
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));

        List<RoomDto> rooms = hotel.getRooms()
                .stream()
                .map(element -> modelMapper.map(element, RoomDto.class))
                .toList();

        if (checkIn != null && checkOut != null && checkOut.isAfter(checkIn)) {
            inventoryService.applyStayPricing(hotelId, rooms, checkIn, checkOut,
                    roomsCount == null || roomsCount < 1 ? 1 : roomsCount);
        }

        return new HotelInfoDto(modelMapper.map(hotel, HotelDto.class), rooms);
    }

    // A hotel owned by someone else is reported as "not found" so its existence is not revealed
    private Hotel getOwnedHotel(Long hotelId) {
        return hotelRepository
                .findByIdAndOwnerId(hotelId, SecurityUtils.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));
    }
}
