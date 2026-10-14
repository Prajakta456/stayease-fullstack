package com.stayease.security;

import com.stayease.IntegrationTestSupport;
import com.stayease.advice.GlobalExceptionHandler;
import com.stayease.entity.User;
import com.stayease.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    // ---------- #1 endpoints require the right role ----------

    @Test
    void adminEndpoints_rejectAnonymousRequests() throws Exception {
        mockMvc.perform(post("/admin/hotels").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"city\":\"Mumbai\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message").value("Authentication required"));
    }

    @Test
    void bookingEndpoints_rejectAnonymousRequests() throws Exception {
        mockMvc.perform(post("/bookings/init").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void guests_cannotUseAdminEndpoints() throws Exception {
        String guest = signup("GUEST");
        mockMvc.perform(get("/admin/hotels").header("Authorization", bearer(guest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void managers_cannotUseBookingEndpoints() throws Exception {
        String manager = signup("HOTEL_MANAGER");
        mockMvc.perform(post("/bookings/init").header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicBrowseEndpoints_stayPublic_evenWithAStaleToken() throws Exception {
        String start = LocalDate.now().plusDays(1).toString();
        String end = LocalDate.now().plusDays(2).toString();
        mockMvc.perform(post("/hotels/search").header("Authorization", "Bearer not-a-real-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\":\"\",\"startDate\":\"" + start + "\",\"endDate\":\"" + end + "\",\"roomsCount\":1}"))
                .andExpect(status().isOk());
    }

    // ---------- #2 tokens are signed, expire and can be revoked ----------

    @Test
    void forgedBase64Token_isRejected() throws Exception {
        String email = uniqueEmail();
        signup("HOTEL_MANAGER", email);
        String forged = Base64.getEncoder().encodeToString(("1:" + email + ":0").getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/admin/hotels").header("Authorization", bearer(forged)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message").value("Invalid authentication token"));
    }

    @Test
    void tamperedJwt_isRejected() throws Exception {
        String token = signup("HOTEL_MANAGER");
        // Change the FIRST signature character: the last base64 character of a 32-byte signature
        // also holds two padding bits, so swapping it (e.g. A -> B) can leave the decoded bytes unchanged
        int signatureStart = token.lastIndexOf('.') + 1;
        char first = token.charAt(signatureStart);
        String tampered = token.substring(0, signatureStart) + (first == 'A' ? 'B' : 'A') + token.substring(signatureStart + 1);

        mockMvc.perform(get("/admin/hotels").header("Authorization", bearer(tampered)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredJwt_isRejected() throws Exception {
        String email = uniqueEmail();
        signup("HOTEL_MANAGER", email);
        User user = userRepository.findByEmail(email).orElseThrow();
        String expired = jwtService.generateToken(user, Duration.ofSeconds(-5));

        mockMvc.perform(get("/admin/hotels").header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message").value("Session expired, please sign in again"));
    }

    @Test
    void logout_revokesPreviouslyIssuedTokens() throws Exception {
        String token = signup("HOTEL_MANAGER");
        mockMvc.perform(get("/admin/hotels").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/admin/hotels").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- #3 signup roles ----------

    @Test
    void signup_defaultsToGuest_whenNoRoleGiven() throws Exception {
        mockMvc.perform(withIp(post("/auth/signup")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"N\",\"email\":\"" + uniqueEmail() + "\",\"password\":\"pw123456\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.roles[0]").value("GUEST"));
    }

    // ---------- #4 managers only see and change their own hotels ----------

    @Test
    void managers_cannotSeeOrChangeAnotherManagersHotel() throws Exception {
        String owner = signup("HOTEL_MANAGER");
        String other = signup("HOTEL_MANAGER");
        long hotelId = createHotel(owner);

        String othersList = mockMvc.perform(get("/admin/hotels").header("Authorization", bearer(other)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<java.util.List<Integer>>read(othersList, "$.data[*].id")).doesNotContain((int) hotelId);

        String path = "/admin/hotels/" + hotelId;
        mockMvc.perform(get(path).header("Authorization", bearer(other))).andExpect(status().isNotFound());
        mockMvc.perform(put(path).header("Authorization", bearer(other)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Hijacked\",\"city\":\"Goa\",\"active\":false}")).andExpect(status().isNotFound());
        mockMvc.perform(patch(path + "/activate").header("Authorization", bearer(other))).andExpect(status().isNotFound());
        mockMvc.perform(post(path + "/rooms").header("Authorization", bearer(other)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"Deluxe\",\"basePrice\":1000,\"totalCount\":2,\"capacity\":2}")).andExpect(status().isNotFound());
        mockMvc.perform(delete(path).header("Authorization", bearer(other))).andExpect(status().isNotFound());

        // still intact for the real owner
        mockMvc.perform(get(path).header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Owned Hotel"));
    }

    // ---------- #6 rooms are scoped to their hotel ----------

    @Test
    void roomEndpoints_rejectARoomFromADifferentHotel() throws Exception {
        String manager = signup("HOTEL_MANAGER");
        long hotelA = createHotel(manager);
        long hotelB = createHotel(manager);
        long roomInA = createRoom(manager, hotelA);

        String wrongPath = "/admin/hotels/" + hotelB + "/rooms/" + roomInA;
        mockMvc.perform(get(wrongPath).header("Authorization", bearer(manager))).andExpect(status().isNotFound());
        mockMvc.perform(delete(wrongPath).header("Authorization", bearer(manager))).andExpect(status().isNotFound());

        mockMvc.perform(get("/admin/hotels/" + hotelA + "/rooms/" + roomInA).header("Authorization", bearer(manager)))
                .andExpect(status().isOk());
    }

    // ---------- #5 bookings belong to the user who made them ----------

    @Test
    void users_cannotAddGuestsToSomeoneElsesBooking() throws Exception {
        String manager = signup("HOTEL_MANAGER");
        long hotelId = createHotel(manager);
        long roomId = createRoom(manager, hotelId);
        mockMvc.perform(patch("/admin/hotels/" + hotelId + "/activate").header("Authorization", bearer(manager)))
                .andExpect(status().isNoContent());

        String alice = signup("GUEST");
        String mallory = signup("GUEST");
        String booking = mockMvc.perform(post("/bookings/init").header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":" + hotelId + ",\"roomId\":" + roomId
                                + ",\"checkInDate\":\"" + LocalDate.now().plusDays(5)
                                + "\",\"checkOutDate\":\"" + LocalDate.now().plusDays(6) + "\",\"roomsCount\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int bookingId = JsonPath.read(booking, "$.data.id");

        String guests = "[{\"name\":\"Guest\",\"gender\":\"FEMALE\",\"age\":30}]";
        mockMvc.perform(post("/bookings/" + bookingId + "/addGuests").header("Authorization", bearer(mallory))
                        .contentType(MediaType.APPLICATION_JSON).content(guests))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/bookings/" + bookingId + "/addGuests").header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON).content(guests))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingStatus").value("GUESTS_ADDED"));
    }

    // ---------- #7 H2 console is not exposed ----------

    @Test
    void h2Console_isNotReachable() throws Exception {
        int status = mockMvc.perform(get("/h2-console/")).andReturn().getResponse().getStatus();
        assertThat(status).isNotEqualTo(200);
    }

    // ---------- #8 internal errors are not leaked ----------

    @Test
    void unexpectedErrors_returnAGenericMessage() {
        var response = new GlobalExceptionHandler()
                .handleInternalServerError(new RuntimeException("could not execute statement [SELECT * FROM app_user]"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getError().getMessage()).isEqualTo("An unexpected error occurred");
    }

    // ---------- #9 login and signup hardening ----------

    @Test
    void login_failsWithTheSame401_forUnknownEmailAndWrongPassword() throws Exception {
        String email = uniqueEmail();
        signup("GUEST", email);

        for (String body : new String[]{
                "{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}",
                "{\"email\":\"" + uniqueEmail() + "\",\"password\":\"pw123456\"}"}) {
            mockMvc.perform(withIp(post("/auth/login")).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message").value("Invalid email or password"));
        }
    }

    @Test
    void duplicateSignup_returns409_withoutEchoingTheEmail() throws Exception {
        String email = uniqueEmail();
        signup("GUEST", email);

        mockMvc.perform(withIp(post("/auth/signup")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"N\",\"email\":\"" + email + "\",\"password\":\"pw123456\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString(email))));
    }

    @Test
    void login_isRateLimitedPerEmail() throws Exception {
        String email = uniqueEmail();
        String body = "{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}";

        for (int attempt = 1; attempt <= 5; attempt++) {
            mockMvc.perform(withIp(post("/auth/login")).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(withIp(post("/auth/login")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void login_isRateLimitedPerIp() throws Exception {
        String ip = randomIp();
        for (int attempt = 1; attempt <= 10; attempt++) {
            mockMvc.perform(withIp(post("/auth/login"), ip).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + uniqueEmail() + "\",\"password\":\"x\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(withIp(post("/auth/login"), ip).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + uniqueEmail() + "\",\"password\":\"x\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
