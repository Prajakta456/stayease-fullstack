package com.stayease.service;

import com.stayease.exception.ConflictException;
import com.stayease.dto.RoomDto;
import com.stayease.entity.Hotel;
import com.stayease.entity.Room;
import com.stayease.exception.ResourceNotFoundException;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.HotelRepository;
import com.stayease.repository.RoomRepository;
import com.stayease.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoomServiceImpl implements RoomService{

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final InventoryService inventoryService;
    private final ModelMapper modelMapper;
    private final BookingRepository bookingRepository;

    @Override
    public RoomDto createNewRoom(Long hotelId, RoomDto roomDto) {
        log.info("Creating a new room in hotel with ID: {}", hotelId);
        Hotel hotel = getOwnedHotel(hotelId);
        Room room = modelMapper.map(roomDto, Room.class);
        room.setId(null);
        room.setHotel(hotel);
        room = roomRepository.save(room);

        if (hotel.getActive()) {
            inventoryService.ensureInventory(room); // same transaction: the room and its inventory are created together
        }

        return modelMapper.map(room, RoomDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDto> getAllRoomsInHotel(Long hotelId) {
        log.info("Getting all rooms in hotel with ID: {}", hotelId);
        Hotel hotel = getOwnedHotel(hotelId);

        return hotel.getRooms()
                .stream()
                .map((element) -> modelMapper.map(element, RoomDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoomDto getRoomById(Long hotelId, Long roomId) {
        log.info("Getting the room with ID: {}", roomId);
        return modelMapper.map(getOwnedRoom(hotelId, roomId), RoomDto.class);
    }

    @Transactional
    @Override
    public void deleteRoomById(Long hotelId, Long roomId) {
        log.info("Deleting the room with ID: {}", roomId);
        Room room = getOwnedRoom(hotelId, roomId);
        if (bookingRepository.existsByRoomId(roomId)) {
            throw new ConflictException("This room has bookings and cannot be deleted");
        }
        inventoryService.deleteAllInventories(room);
        roomRepository.deleteById(roomId);
    }

    // Hotels/rooms of other managers are reported as "not found" so their existence is not revealed
    private Hotel getOwnedHotel(Long hotelId) {
        return hotelRepository
                .findByIdAndOwnerId(hotelId, SecurityUtils.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: "+hotelId));
    }

    private Room getOwnedRoom(Long hotelId, Long roomId) {
        Hotel hotel = getOwnedHotel(hotelId);
        return roomRepository
                .findByIdAndHotelId(roomId, hotel.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: "+roomId));
    }
}
