
package com.codingshuttle.projects.airBnbApp.repository;

import com.codingshuttle.projects.airBnbApp.dto.HotelPriceDto;
import com.codingshuttle.projects.airBnbApp.entity.Hotel;
import com.codingshuttle.projects.airBnbApp.entity.Inventory;
import com.codingshuttle.projects.airBnbApp.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    void deleteByRoom(Room room);

    // ✅ NEW: search directly from Inventory (not HotelMinPrice which needs cron to populate).
    // Returns HotelPriceDto with avg nightly price. city='' means all cities.
    @Query("""
            SELECT new com.codingshuttle.projects.airBnbApp.dto.HotelPriceDto(i.hotel, CAST(AVG(i.price) AS double))
            FROM Inventory i
            WHERE (:city = '' OR i.city = :city)
                AND i.date >= :startDate
                AND i.date < :endDate
                AND i.closed = false
                AND (i.totalCount - i.bookedCount - i.reservedCount) >= :roomsCount
                AND i.hotel.active = true
            GROUP BY i.hotel
            HAVING COUNT(i.date) >= :dateCount
            """)
    Page<HotelPriceDto> searchHotelsWithPrice(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("dateCount") Long dateCount,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT i.hotel
            FROM Inventory i
            WHERE i.city = :city
                AND i.date BETWEEN :startDate AND :endDate
                AND i.closed = false
                AND (i.totalCount - i.bookedCount - i.reservedCount) >= :roomsCount
           GROUP BY i.hotel, i.room
           HAVING COUNT(i.date) = :dateCount
           """)
    Page<Hotel> findHotelsWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("dateCount") Long dateCount,
            Pageable pageable
            );

    // ✅ FIXED: date < :checkOutDate (not BETWEEN ... checkOutDate) — checkout day is not a stay night
    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.room.id = :roomId
                AND i.date >= :checkInDate
                AND i.date < :checkOutDate
                AND i.closed = false
                AND (i.totalCount - i.bookedCount - i.reservedCount) >= :roomsCount
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAndLockAvailableInventory(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("roomsCount") Integer roomsCount
    );

    List<Inventory> findByHotelAndDateBetween(Hotel hotel, LocalDate startDate, LocalDate endDate);
}   