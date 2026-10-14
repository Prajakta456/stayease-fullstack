package com.stayease;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the app against a real MySQL server: the Flyway migrations must apply cleanly, Hibernate's
 * schema validation must pass, and the MySQL-specific paths (native search SQL, row locks) must work.
 * Skipped automatically when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "app.seed-demo-data=true")
@AutoConfigureMockMvc
class MySqlSchemaTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void migrationsValidate_andTheDemoFlowWorksOnMySql() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(20);
        LocalDate checkOut = checkIn.plusDays(2);

        // native search SQL
        String search = mockMvc.perform(post("/hotels/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\":\"goa\",\"startDate\":\"" + checkIn + "\",\"endDate\":\"" + checkOut
                                + "\",\"roomsCount\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].hotel.name").value("Sea Breeze Resort"))
                .andExpect(jsonPath("$.data.content[0].price").value(2500.0))
                .andReturn().getResponse().getContentAsString();
        int hotelId = JsonPath.read(search, "$.data.content[0].hotel.id");

        // seeded demo login
        String login = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"guest@demo.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(login, "$.data.token");

        String info = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/hotels/" + hotelId + "/info"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int roomId = JsonPath.read(info, "$.data.rooms[0].id");

        // pessimistic row locks + inventory updates
        mockMvc.perform(post("/bookings/init").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":" + hotelId + ",\"roomId\":" + roomId + ",\"checkInDate\":\"" + checkIn
                                + "\",\"checkOutDate\":\"" + checkOut + "\",\"roomsCount\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingStatus").value("RESERVED"));
    }
}
