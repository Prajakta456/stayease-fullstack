package com.stayease.repository;

import com.stayease.entity.Hotel;
import com.stayease.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Modifying
    @Query("DELETE FROM Inventory i WHERE i.room.id = :roomId")
    void deleteAllByRoomId(@Param("roomId") Long roomId);

    // Inventory keeps a copy of the city for fast search; keep it in sync when a hotel moves
    @Modifying
    @Query("UPDATE Inventory i SET i.city = :city WHERE i.hotel.id = :hotelId")
    void updateCityForHotel(@Param("hotelId") Long hotelId, @Param("city") String city);

    @Query("SELECT i.date FROM Inventory i WHERE i.room.id = :roomId AND i.date BETWEEN :from AND :to")
    List<LocalDate> findDatesForRoom(@Param("roomId") Long roomId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT i FROM Inventory i
            WHERE i.hotel.id = :hotelId AND i.date >= :startDate AND i.date < :endDate
            """)
    List<Inventory> findForHotelStay(@Param("hotelId") Long hotelId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    /*
     * The search text matches the city exactly or the hotel name partially (case-insensitive).
     * A hotel matches when at least one of its room types has enough free rooms on EVERY
     * night of the stay. The inner query finds those room types (one row per room with its
     * average nightly price); the outer query keeps the cheapest one per hotel.
     * Native SQL because JPQL cannot aggregate over a grouped subquery.
     */
    String BOOKABLE_ROOMS_SQL = """
            SELECT i.hotel_id, i.room_id, AVG(i.price) AS avg_price
            FROM inventory i
            JOIN hotel h ON h.id = i.hotel_id
            WHERE (:city = '' OR LOWER(i.city) = LOWER(:city) OR LOWER(h.name) LIKE CONCAT('%', LOWER(:city), '%'))
                AND i.date >= :startDate
                AND i.date < :endDate
                AND i.closed = false
                AND (i.total_count - i.booked_count - i.reserved_count) >= :roomsCount
                AND h.active = true
            GROUP BY i.hotel_id, i.room_id
            HAVING COUNT(*) = :dateCount
            """;

    @Query(value = "SELECT r.hotel_id AS hotelId, MIN(r.avg_price) AS price FROM ("
                    + BOOKABLE_ROOMS_SQL
                    + ") r GROUP BY r.hotel_id ORDER BY price, r.hotel_id",
            countQuery = "SELECT COUNT(DISTINCT r.hotel_id) FROM (" + BOOKABLE_ROOMS_SQL + ") r",
            nativeQuery = true)
    Page<HotelPriceProjection> searchHotelsWithPrice(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("dateCount") Long dateCount,
            Pageable pageable
    );

    // Nights of a stay: check-in date inclusive, check-out date exclusive.
    // Rows are locked in date order so concurrent bookings never deadlock.
    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.room.id = :roomId
                AND i.date >= :checkInDate
                AND i.date < :checkOutDate
                AND i.closed = false
                AND (i.totalCount - i.bookedCount - i.reservedCount) >= :roomsCount
            ORDER BY i.date
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAndLockAvailableInventory(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("roomsCount") Integer roomsCount
    );

    // All nights of an existing booking, regardless of availability (used to release or confirm rooms)
    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.room.id = :roomId
                AND i.date >= :checkInDate
                AND i.date < :checkOutDate
            ORDER BY i.date
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAndLockInventoryForStay(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    List<Inventory> findByHotelAndDateBetween(Hotel hotel, LocalDate startDate, LocalDate endDate);

    // Touches only the price column, so it can never overwrite booked/reserved counts that a
    // booking changed after the pricing job read the row
    @Modifying
    @Query("UPDATE Inventory i SET i.price = :price WHERE i.id = :id")
    void updatePrice(@Param("id") Long id, @Param("price") BigDecimal price);
}
