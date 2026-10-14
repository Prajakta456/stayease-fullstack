package com.stayease.repository;

import com.stayease.entity.Booking;
import com.stayease.entity.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Every state change locks the booking row first (then its inventory rows), so the
    // expiry job, payment and cancellation can never act on the same booking concurrently.

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id AND b.user.id = :userId")
    Optional<Booking> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT b.id FROM Booking b WHERE b.bookingStatus IN :statuses AND b.createdAt < :cutoff")
    List<Long> findIdsByStatusInAndCreatedAtBefore(@Param("statuses") Collection<BookingStatus> statuses,
                                                    @Param("cutoff") LocalDateTime cutoff);

    boolean existsByHotelId(Long hotelId);

    boolean existsByRoomId(Long roomId);

    @EntityGraph(attributePaths = {"hotel", "room", "guests"})
    List<Booking> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
