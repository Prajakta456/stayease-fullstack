package com.stayease.booking;

import com.stayease.IntegrationTestSupport;
import com.stayease.service.BookingExpiryScheduler;
import com.stayease.service.PricingUpdateService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingFlowIntegrationTest extends IntegrationTestSupport {

    private static final String ONE_GUEST = "[{\"name\":\"Asha\",\"gender\":\"FEMALE\",\"age\":30}]";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BookingExpiryScheduler bookingExpiryScheduler;

    @Autowired
    private PricingUpdateService pricingUpdateService;

    private String manager;
    private String guest;
    private long hotelId;
    private long roomId;
    private final LocalDate checkIn = LocalDate.now().plusDays(3);
    private final LocalDate checkOut = LocalDate.now().plusDays(5); // 2 nights

    @BeforeEach
    void setUp() throws Exception {
        manager = signup("HOTEL_MANAGER");
        guest = signup("GUEST");
        hotelId = createHotel(manager);
        roomId = createRoom(manager, hotelId, 1000, 2, 2); // 2 rooms of this type, 2 guests each
        activateHotel(manager, hotelId);
    }

    // ---------- #10 / #15 request validation ----------

    @Test
    void negativeOrZeroRoomsCount_isRejected_andInventoryIsUntouched() throws Exception {
        for (int roomsCount : new int[]{-50, 0}) {
            initBooking(guest, hotelId, roomId, checkIn, checkOut, roomsCount)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.subErrors", hasItem(containsString("roomsCount"))));
        }
        assertThat(reservedCount(roomId, checkIn)).isZero();
    }

    @Test
    void missingFields_areRejected() throws Exception {
        mockMvc.perform(post("/bookings/init").header("Authorization", bearer(guest))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.subErrors.length()").value(5));
    }

    @Test
    void pastCheckInDate_isRejected() throws Exception {
        initBooking(guest, hotelId, roomId, LocalDate.now().minusDays(2), LocalDate.now().plusDays(1), 1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.subErrors", hasItem(containsString("checkInDate"))));
    }

    @Test
    void checkOutBeforeCheckIn_isRejected() throws Exception {
        initBooking(guest, hotelId, roomId, checkOut, checkIn, 1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Check-out date must be after check-in date"));
    }

    @Test
    void guests_mustBeBetweenOneAndRoomCapacity() throws Exception {
        long bookingId = reserve(1);

        addGuests(bookingId, "[]")
                .andExpect(status().isBadRequest());
        // capacity 2 x 1 room
        addGuests(bookingId, "[" + String.join(",", java.util.Collections.nCopies(3,
                        "{\"name\":\"G\",\"gender\":\"MALE\",\"age\":20}")) + "]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("This booking allows at most 2 guests"));
        addGuests(bookingId, "[{\"name\":\"\",\"age\":-1}]")
                .andExpect(status().isBadRequest());

        addGuests(bookingId, ONE_GUEST).andExpect(status().isOk());
    }

    // ---------- #14 room/hotel consistency ----------

    @Test
    void roomFromAnotherHotel_isRejected() throws Exception {
        long otherHotel = createHotel(manager);
        long otherRoom = createRoom(manager, otherHotel);
        activateHotel(manager, otherHotel);

        initBooking(guest, hotelId, otherRoom, checkIn, checkOut, 1)
                .andExpect(status().isNotFound());
    }

    @Test
    void inactiveHotel_cannotBeBooked() throws Exception {
        mockMvc.perform(patch("/admin/hotels/" + hotelId + "/deactivate").header("Authorization", bearer(manager)))
                .andExpect(status().isNoContent());

        initBooking(guest, hotelId, roomId, checkIn, checkOut, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("This hotel is not accepting bookings"));
    }

    // ---------- #12 full lifecycle: reserve -> guests -> pay -> cancel ----------

    @Test
    void fullBookingFlow_movesRoomsFromReservedToBooked() throws Exception {
        long bookingId = reserve(2);
        assertThat(reservedCount(roomId, checkIn)).isEqualTo(2);

        mockMvc.perform(post("/bookings/" + bookingId + "/payments").header("Authorization", bearer(guest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("Add guest details before paying"));

        addGuests(bookingId, ONE_GUEST).andExpect(status().isOk());

        mockMvc.perform(post("/bookings/" + bookingId + "/payments").header("Authorization", bearer(guest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.amount").value(4000.0)) // 1000 x 2 nights x 2 rooms
                .andExpect(jsonPath("$.data.holdExpiresAt").doesNotExist());

        assertThat(reservedCount(roomId, checkIn)).isZero();
        assertThat(bookedCount(roomId, checkIn)).isEqualTo(2);
        assertThat(bookedCount(roomId, checkIn.plusDays(1))).isEqualTo(2);
        assertThat(bookedCount(roomId, checkOut)).as("check-out day is not a stay night").isZero();
        assertThat(paymentStatus(bookingId)).isEqualTo("CONFIRMED");

        // paying twice is rejected
        mockMvc.perform(post("/bookings/" + bookingId + "/payments").header("Authorization", bearer(guest)))
                .andExpect(status().isConflict());

        // the hotel is now full for those nights
        initBooking(signup("GUEST"), hotelId, roomId, checkIn, checkOut, 1)
                .andExpect(status().isConflict());
    }

    @Test
    void cancellingAConfirmedBooking_releasesRoomsAndRefunds() throws Exception {
        long bookingId = reserve(1);
        addGuests(bookingId, ONE_GUEST).andExpect(status().isOk());
        mockMvc.perform(post("/bookings/" + bookingId + "/payments").header("Authorization", bearer(guest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/bookings/" + bookingId + "/cancel").header("Authorization", bearer(guest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingStatus").value("CANCELLED"));

        assertThat(bookedCount(roomId, checkIn)).isZero();
        assertThat(paymentStatus(bookingId)).isEqualTo("CANCELLED");

        mockMvc.perform(post("/bookings/" + bookingId + "/cancel").header("Authorization", bearer(guest)))
                .andExpect(status().isConflict());
    }

    @Test
    void cancellingAHold_releasesReservedRooms() throws Exception {
        long bookingId = reserve(2);

        mockMvc.perform(post("/bookings/" + bookingId + "/cancel").header("Authorization", bearer(guest)))
                .andExpect(status().isOk());

        assertThat(reservedCount(roomId, checkIn)).isZero();
    }

    @Test
    void myBookings_listsOnlyTheCallersBookings() throws Exception {
        long bookingId = reserve(1);
        signup("GUEST"); // another user with no bookings

        String response = mockMvc.perform(get("/bookings").header("Authorization", bearer(guest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].hotelName").value("Owned Hotel"))
                .andExpect(jsonPath("$.data[0].roomType").value("Deluxe"))
                .andReturn().getResponse().getContentAsString();
        assertThat(((Number) JsonPath.read(response, "$.data[0].id")).longValue()).isEqualTo(bookingId);

        mockMvc.perform(get("/bookings").header("Authorization", bearer(signup("GUEST"))))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ---------- #11 unpaid holds expire and release their rooms ----------

    @Test
    void elapsedHolds_expireAndReleaseRooms_exactlyOnce() throws Exception {
        long bookingId = reserve(2);
        addGuests(bookingId, ONE_GUEST).andExpect(status().isOk());
        backdateBooking(bookingId, 11);

        bookingExpiryScheduler.expireElapsedHolds();
        bookingExpiryScheduler.expireElapsedHolds(); // a second run must not release the rooms again

        assertThat(bookingStatus(bookingId)).isEqualTo("EXPIRED");
        assertThat(reservedCount(roomId, checkIn)).isZero();

        mockMvc.perform(post("/bookings/" + bookingId + "/payments").header("Authorization", bearer(guest)))
                .andExpect(status().isConflict());
    }

    @Test
    void activeHolds_areNotExpired() throws Exception {
        long bookingId = reserve(1);

        bookingExpiryScheduler.expireElapsedHolds();

        assertThat(bookingStatus(bookingId)).isEqualTo("RESERVED");
        assertThat(reservedCount(roomId, checkIn)).isEqualTo(1);
    }

    @Test
    void elapsedHold_cannotBePaid_evenBeforeTheJobRuns() throws Exception {
        long bookingId = reserve(1);
        addGuests(bookingId, ONE_GUEST).andExpect(status().isOk());
        backdateBooking(bookingId, 11);

        mockMvc.perform(post("/bookings/" + bookingId + "/payments").header("Authorization", bearer(guest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("Your reservation hold has expired, please book again"));
    }

    // ---------- #13 pricing job only touches prices ----------

    @Test
    void pricingJob_updatesPricesWithoutTouchingRoomCounts() throws Exception {
        long confirmed = reserve(1);
        addGuests(confirmed, ONE_GUEST).andExpect(status().isOk());
        mockMvc.perform(post("/bookings/" + confirmed + "/payments").header("Authorization", bearer(guest)))
                .andExpect(status().isOk());
        reserve(1); // a second, unpaid hold

        pricingUpdateService.updatePrices();

        assertThat(bookedCount(roomId, checkIn)).isEqualTo(1);
        assertThat(reservedCount(roomId, checkIn)).isEqualTo(1);
        // stay starts within 7 days, so the urgency surcharge applies on top of the 1000 base price
        assertThat(jdbcTemplate.queryForObject("SELECT price FROM inventory WHERE room_id = ? AND date = ?",
                java.math.BigDecimal.class, roomId, checkIn)).isGreaterThan(java.math.BigDecimal.valueOf(1000));
    }

    // ---------- #16 search requires one room type free on every night ----------

    @Test
    void search_skipsHotelsWhereNoSingleRoomTypeIsFreeForTheWholeStay() throws Exception {
        String city = "City-" + UUID.randomUUID();
        long split = createHotel(manager, city);
        long roomA = createRoom(manager, split, 1000, 1, 2);
        long roomB = createRoom(manager, split, 3000, 1, 2);
        activateHotel(manager, split);

        // room A is taken on night 2, room B on night 1
        initBooking(guest, split, roomA, checkIn.plusDays(1), checkOut, 1).andExpect(status().isOk());
        initBooking(guest, split, roomB, checkIn, checkIn.plusDays(1), 1).andExpect(status().isOk());

        search(city, checkIn, checkOut, 1)
                .andExpect(jsonPath("$.data.content.length()").value(0));

        // a single night where room B is still free -> found, priced at room B
        search(city, checkIn.plusDays(1), checkOut, 1)
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].price").value(3000.0));
    }

    @Test
    void search_showsTheCheapestBookableRoomType_andMatchesCityCaseInsensitively() throws Exception {
        String city = "City-" + UUID.randomUUID();
        long hotel = createHotel(manager, city);
        createRoom(manager, hotel, 5000, 1, 2);
        createRoom(manager, hotel, 2000, 1, 2);
        activateHotel(manager, hotel);

        search(city.toUpperCase(), checkIn, checkOut, 1)
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].hotel.id").value(hotel))
                .andExpect(jsonPath("$.data.content[0].price").value(2000.0))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        // more rooms than any single type has -> no match
        search(city, checkIn, checkOut, 2)
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    // ---------- helpers ----------

    private long reserve(int roomsCount) throws Exception {
        String response = initBooking(guest, hotelId, roomId, checkIn, checkOut, roomsCount)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingStatus").value("RESERVED"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    private org.springframework.test.web.servlet.ResultActions addGuests(long bookingId, String guestsJson) throws Exception {
        return mockMvc.perform(post("/bookings/" + bookingId + "/addGuests").header("Authorization", bearer(guest))
                .contentType(MediaType.APPLICATION_JSON).content(guestsJson));
    }

    private org.springframework.test.web.servlet.ResultActions search(String city, LocalDate start, LocalDate end,
                                                                      int roomsCount) throws Exception {
        return mockMvc.perform(post("/hotels/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\":\"" + city + "\",\"startDate\":\"" + start + "\",\"endDate\":\"" + end
                                + "\",\"roomsCount\":" + roomsCount + "}"))
                .andExpect(status().isOk());
    }

    private void backdateBooking(long bookingId, int minutesAgo) {
        jdbcTemplate.update("UPDATE booking SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(minutesAgo)), bookingId);
    }

    private int reservedCount(long room, LocalDate date) {
        return jdbcTemplate.queryForObject(
                "SELECT reserved_count FROM inventory WHERE room_id = ? AND date = ?", Integer.class, room, date);
    }

    private int bookedCount(long room, LocalDate date) {
        return jdbcTemplate.queryForObject(
                "SELECT booked_count FROM inventory WHERE room_id = ? AND date = ?", Integer.class, room, date);
    }

    private String bookingStatus(long bookingId) {
        return jdbcTemplate.queryForObject("SELECT booking_status FROM booking WHERE id = ?", String.class, bookingId);
    }

    private String paymentStatus(long bookingId) {
        List<String> statuses = jdbcTemplate.queryForList(
                "SELECT payment_status FROM payment WHERE booking_id = ?", String.class, bookingId);
        assertThat(statuses).hasSize(1);
        return statuses.get(0);
    }
}
