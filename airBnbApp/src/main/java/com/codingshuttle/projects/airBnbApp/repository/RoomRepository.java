package com.codingshuttle.projects.airBnbApp.repository;

import com.codingshuttle.projects.airBnbApp.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByIdAndHotelId(Long id, Long hotelId);

    @Query("SELECT r.id FROM Room r WHERE r.hotel.active = true")
    List<Long> findIdsOfRoomsInActiveHotels();
}
