package com.stayease.repository;

import com.stayease.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByIdAndHotelId(Long id, Long hotelId);

    @Query("SELECT r.id FROM Room r WHERE r.hotel.active = true")
    List<Long> findIdsOfRoomsInActiveHotels();
}
