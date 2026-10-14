package com.stayease.repository;

import com.stayease.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findAllByOwnerId(Long ownerId);

    Optional<Hotel> findByIdAndOwnerId(Long id, Long ownerId);
}
