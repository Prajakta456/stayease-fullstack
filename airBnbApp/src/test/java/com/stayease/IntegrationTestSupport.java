package com.stayease;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared helpers for full-stack (MockMvc + H2) tests. Every helper creates fresh, uniquely named data. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    protected String signup(String role) throws Exception {
        return signup(role, uniqueEmail());
    }

    protected String signup(String role, String email) throws Exception {
        String response = mockMvc.perform(withIp(post("/auth/signup")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"email\":\"" + email + "\",\"password\":\"pw123456\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.data.token");
    }

    protected long createHotel(String managerToken) throws Exception {
        return createHotel(managerToken, "Mumbai");
    }

    protected long createHotel(String managerToken, String city) throws Exception {
        String response = mockMvc.perform(post("/admin/hotels").header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Owned Hotel\",\"city\":\"" + city + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    protected long createRoom(String managerToken, long hotelId) throws Exception {
        return createRoom(managerToken, hotelId, 1000, 2, 2);
    }

    protected long createRoom(String managerToken, long hotelId, int basePrice, int totalCount, int capacity) throws Exception {
        String response = mockMvc.perform(post("/admin/hotels/" + hotelId + "/rooms")
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"Deluxe\",\"basePrice\":" + basePrice + ",\"totalCount\":" + totalCount
                                + ",\"capacity\":" + capacity + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    protected void activateHotel(String managerToken, long hotelId) throws Exception {
        mockMvc.perform(patch("/admin/hotels/" + hotelId + "/activate").header("Authorization", bearer(managerToken)))
                .andExpect(status().isNoContent());
    }

    protected ResultActions initBooking(String guestToken, long hotelId, long roomId,
                                        LocalDate checkIn, LocalDate checkOut, Object roomsCount) throws Exception {
        return mockMvc.perform(post("/bookings/init").header("Authorization", bearer(guestToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hotelId\":" + hotelId + ",\"roomId\":" + roomId
                        + ",\"checkInDate\":\"" + checkIn + "\",\"checkOutDate\":\"" + checkOut
                        + "\",\"roomsCount\":" + roomsCount + "}"));
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.local";
    }

    /** A fresh client IP per request keeps the per-IP rate limits from leaking between tests. */
    protected static MockHttpServletRequestBuilder withIp(MockHttpServletRequestBuilder builder) {
        return withIp(builder, randomIp());
    }

    protected static MockHttpServletRequestBuilder withIp(MockHttpServletRequestBuilder builder, String ip) {
        RequestPostProcessor remoteAddr = request -> {
            request.setRemoteAddr(ip);
            return request;
        };
        return builder.with(remoteAddr);
    }

    protected static String randomIp() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return "10." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(1, 255);
    }
}
