package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.dto.HotelDto;
import com.codingshuttle.projects.airBnbApp.dto.HotelPriceDto;
import com.codingshuttle.projects.airBnbApp.dto.HotelSearchRequest;
import com.codingshuttle.projects.airBnbApp.dto.RoomDto;
import com.codingshuttle.projects.airBnbApp.entity.Hotel;
import com.codingshuttle.projects.airBnbApp.entity.Inventory;
import com.codingshuttle.projects.airBnbApp.entity.Room;
import com.codingshuttle.projects.airBnbApp.exception.ResourceNotFoundException;
import com.codingshuttle.projects.airBnbApp.repository.HotelPriceProjection;
import com.codingshuttle.projects.airBnbApp.repository.HotelRepository;
import com.codingshuttle.projects.airBnbApp.repository.InventoryRepository;
import com.codingshuttle.projects.airBnbApp.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ModelMapper modelMapper;
    private final InventoryRepository inventoryRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public void ensureInventory(Room room) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);
        Set<LocalDate> existing = new HashSet<>(inventoryRepository.findDatesForRoom(room.getId(), today, endDate));

        List<Inventory> missing = new ArrayList<>();
        for (LocalDate date = today; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (existing.contains(date)) {
                continue;
            }
            missing.add(Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .bookedCount(0)
                    .reservedCount(0)
                    .city(room.getHotel().getCity())
                    .date(date)
                    .price(room.getBasePrice())
                    .surgeFactor(BigDecimal.ONE)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build());
        }
        inventoryRepository.saveAll(missing);
        if (!missing.isEmpty()) {
            log.info("Created {} inventory rows for room {}", missing.size(), room.getId());
        }
    }

    @Override
    @Transactional
    public void ensureInventory(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));
        ensureInventory(room);
    }

    @Override
    @Transactional
    public void deleteAllInventories(Room room) {
        log.info("Deleting the inventories of room with id: {}", room.getId());
        inventoryRepository.deleteAllByRoomId(room.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HotelPriceDto> searchHotels(HotelSearchRequest hotelSearchRequest) {
        log.info("Searching hotels for '{}' city, from {} to {}",
                hotelSearchRequest.getCity(),
                hotelSearchRequest.getStartDate(),
                hotelSearchRequest.getEndDate());

        int page = Math.max(0, hotelSearchRequest.getPage() == null ? 0 : hotelSearchRequest.getPage());
        int size = Math.min(MAX_PAGE_SIZE, Math.max(1, hotelSearchRequest.getSize() == null ? 10 : hotelSearchRequest.getSize()));
        Pageable pageable = PageRequest.of(page, size);

        // Check-out day is not a stay night
        long dateCount = ChronoUnit.DAYS.between(
                hotelSearchRequest.getStartDate(),
                hotelSearchRequest.getEndDate());

        // Null/blank means "anywhere"; otherwise matches a city or part of a hotel name
        String city = (hotelSearchRequest.getCity() == null) ? "" : hotelSearchRequest.getCity().trim();

        // Queried from Inventory directly, so newly activated hotels show up immediately
        Page<HotelPriceProjection> matches = inventoryRepository.searchHotelsWithPrice(
                city,
                hotelSearchRequest.getStartDate(),
                hotelSearchRequest.getEndDate(),
                hotelSearchRequest.getRoomsCount(),
                dateCount,
                pageable);

        Map<Long, Hotel> hotelsById = hotelRepository
                .findAllById(matches.map(HotelPriceProjection::getHotelId).getContent())
                .stream()
                .collect(Collectors.toMap(Hotel::getId, Function.identity()));

        // Keep the query's cheapest-first order
        return matches.map(match -> new HotelPriceDto(
                modelMapper.map(hotelsById.get(match.getHotelId()), HotelDto.class),
                match.getPrice().setScale(2, RoundingMode.HALF_UP)));
    }

    @Override
    @Transactional(readOnly = true)
    public void applyStayPricing(Long hotelId, List<RoomDto> rooms, LocalDate checkIn, LocalDate checkOut, int roomsCount) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        Map<Long, List<Inventory>> nightsByRoom = inventoryRepository.findForHotelStay(hotelId, checkIn, checkOut)
                .stream()
                .collect(Collectors.groupingBy(inventory -> inventory.getRoom().getId()));

        for (RoomDto room : rooms) {
            List<Inventory> roomNights = nightsByRoom.getOrDefault(room.getId(), List.of());
            boolean allNightsExist = nights > 0 && roomNights.size() == nights;
            room.setAvailable(allNightsExist && roomNights.stream().allMatch(night -> !night.getClosed()
                    && night.getTotalCount() - night.getBookedCount() - night.getReservedCount() >= roomsCount));
            if (allNightsExist) {
                BigDecimal total = roomNights.stream().map(Inventory::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                room.setPrice(total.divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP));
            }
        }
    }
}
