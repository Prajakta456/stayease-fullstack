package com.codingshuttle.projects.airBnbApp.hotel;

import com.codingshuttle.projects.airBnbApp.IntegrationTestSupport;
import com.codingshuttle.projects.airBnbApp.service.InventoryExtensionScheduler;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HotelInventoryIntegrationTest extends IntegrationTestSupport {

    // today .. today + 1 year, inclusive
    private static final int HORIZON_ROWS =
            (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.now().plusYears(1)) + 1;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InventoryExtensionScheduler inventoryExtensionScheduler;

    private String manager;
    private String city;
    private long hotelId;
    private long roomId;
    private final LocalDate checkIn = LocalDate.now().plusDays(10);
    private final LocalDate checkOut = LocalDate.now().plusDays(12);

    @BeforeEach
    void setUp() throws Exception {
        manager = signup("HOTEL_MANAGER");
        city = "City-" + UUID.randomUUID();
        hotelId = createHotel(manager, city);
        roomId = createRoom(manager, hotelId, 1000, 1, 2);
    }

    // ---------- activation ----------

    @Test
    void activatingTwice_isIdempotent() throws Exception {
        activateHotel(manager, hotelId);
        activateHotel(manager, hotelId);

        assertThat(inventoryRows(roomId)).isEqualTo(HORIZON_ROWS);
    }

    @Test
    void roomsAddedToAnActiveHotel_getInventoryImmediately() throws Exception {
        activateHotel(manager, hotelId);
        long laterRoom = createRoom(manager, hotelId, 2000, 1, 2);

        assertThat(inventoryRows(laterRoom)).isEqualTo(HORIZON_ROWS);
    }

    @Test
    void deactivatedHotels_disappearFromSearchAndPublicPages_andComeBackOnReactivation() throws Exception {
        activateHotel(manager, hotelId);
        searchCount(city).andExpect(jsonPath("$.data.content.length()").value(1));

        mockMvc.perform(patch("/admin/hotels/" + hotelId + "/deactivate").header("Authorization", bearer(manager)))
                .andExpect(status().isNoContent());
        searchCount(city).andExpect(jsonPath("$.data.content.length()").value(0));
        mockMvc.perform(get("/hotels/" + hotelId + "/info")).andExpect(status().isNotFound());

        activateHotel(manager, hotelId);
        searchCount(city).andExpect(jsonPath("$.data.content.length()").value(1));
        assertThat(inventoryRows(roomId)).as("no duplicate rows after reactivation").isEqualTo(HORIZON_ROWS);
    }

    @Test
    void unpublishedHotels_areNotPublic() throws Exception {
        mockMvc.perform(get("/hotels/" + hotelId + "/info")).andExpect(status().isNotFound());
    }

    // ---------- updates ----------

    @Test
    void update_cannotChangeActive_andKeepsOmittedFields() throws Exception {
        mockMvc.perform(put("/admin/hotels/" + hotelId).header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amenities\":[\"Pool\"],\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Owned Hotel"))
                .andExpect(jsonPath("$.data.city").value(city))
                .andExpect(jsonPath("$.data.amenities[0]").value("Pool"))
                .andExpect(jsonPath("$.data.active").value(false));

        assertThat(inventoryRows(roomId)).isZero();
    }

    @Test
    void changingTheCity_movesTheHotelInSearch() throws Exception {
        activateHotel(manager, hotelId);
        String newCity = "City-" + UUID.randomUUID();

        mockMvc.perform(put("/admin/hotels/" + hotelId).header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"city\":\"" + newCity + "\"}"))
                .andExpect(status().isOk());

        searchCount(city).andExpect(jsonPath("$.data.content.length()").value(0));
        searchCount(newCity).andExpect(jsonPath("$.data.content.length()").value(1));
    }

    // ---------- deletion ----------

    @Test
    void hotelsAndRoomsWithBookings_cannotBeDeleted() throws Exception {
        activateHotel(manager, hotelId);
        initBooking(signup("GUEST"), hotelId, roomId, checkIn, checkOut, 1).andExpect(status().isOk());

        mockMvc.perform(delete("/admin/hotels/" + hotelId).header("Authorization", bearer(manager)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("This hotel has bookings and cannot be deleted; deactivate it instead"));
        mockMvc.perform(delete("/admin/hotels/" + hotelId + "/rooms/" + roomId).header("Authorization", bearer(manager)))
                .andExpect(status().isConflict());
    }

    @Test
    void hotelsWithoutBookings_areDeletedWithTheirInventory() throws Exception {
        activateHotel(manager, hotelId);

        mockMvc.perform(delete("/admin/hotels/" + hotelId).header("Authorization", bearer(manager)))
                .andExpect(status().isNoContent());

        assertThat(inventoryRows(roomId)).isZero();
        mockMvc.perform(get("/admin/hotels/" + hotelId).header("Authorization", bearer(manager)))
                .andExpect(status().isNotFound());
    }

    // ---------- inventory horizon ----------

    @Test
    void dailyJob_rollsTheInventoryHorizonForward() throws Exception {
        activateHotel(manager, hotelId);
        // simulate time passing: the last 30 days of the horizon are missing
        jdbcTemplate.update("DELETE FROM inventory WHERE room_id = ? AND date > ?", roomId, LocalDate.now().plusDays(335));

        inventoryExtensionScheduler.extendInventory();

        assertThat(inventoryRows(roomId)).isEqualTo(HORIZON_ROWS);
    }

    // ---------- public hotel page ----------

    @Test
    void hotelInfo_withDates_showsPerRoomPriceAndAvailability() throws Exception {
        long suite = createRoom(manager, hotelId, 3000, 1, 2);
        activateHotel(manager, hotelId);
        // the suite gets booked for the first night
        initBooking(signup("GUEST"), hotelId, suite, checkIn, checkIn.plusDays(1), 1).andExpect(status().isOk());

        String response = mockMvc.perform(get("/hotels/" + hotelId + "/info")
                        .param("startDate", checkIn.toString())
                        .param("endDate", checkOut.toString())
                        .param("roomsCount", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<java.util.List<Boolean>>read(response, "$.data.rooms[?(@.id == " + roomId + ")].available"))
                .containsExactly(true);
        assertThat(JsonPath.<java.util.List<Double>>read(response, "$.data.rooms[?(@.id == " + roomId + ")].price"))
                .containsExactly(1000.0);
        assertThat(JsonPath.<java.util.List<Boolean>>read(response, "$.data.rooms[?(@.id == " + suite + ")].available"))
                .containsExactly(false);

        // without dates, no stay pricing is attached
        mockMvc.perform(get("/hotels/" + hotelId + "/info"))
                .andExpect(jsonPath("$.data.rooms[0].price").doesNotExist())
                .andExpect(jsonPath("$.data.rooms[0].available").doesNotExist());
    }

    // ---------- search ----------

    @Test
    void search_matchesPartOfTheHotelName() throws Exception {
        String name = "Lagoon-" + UUID.randomUUID();
        mockMvc.perform(put("/admin/hotels/" + hotelId).header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name + " Resort\"}"))
                .andExpect(status().isOk());
        activateHotel(manager, hotelId);

        searchCount(name.toLowerCase())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].hotel.name").value(name + " Resort"))
                .andExpect(jsonPath("$.data.content[0].hotel.createdAt").doesNotExist());
    }

    @Test
    void search_capsThePageSize() throws Exception {
        mockMvc.perform(post("/hotels/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\":\"\",\"startDate\":\"" + checkIn + "\",\"endDate\":\"" + checkOut
                                + "\",\"roomsCount\":1,\"page\":-3,\"size\":100000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(50))
                .andExpect(jsonPath("$.data.number").value(0));
    }

    // ---------- helpers ----------

    private ResultActions searchCount(String query) throws Exception {
        return mockMvc.perform(post("/hotels/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\":\"" + query + "\",\"startDate\":\"" + checkIn + "\",\"endDate\":\"" + checkOut
                                + "\",\"roomsCount\":1}"))
                .andExpect(status().isOk());
    }

    private int inventoryRows(long room) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory WHERE room_id = ?", Integer.class, room);
    }
}
