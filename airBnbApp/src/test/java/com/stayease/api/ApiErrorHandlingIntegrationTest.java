package com.stayease.api;

import com.stayease.IntegrationTestSupport;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Client mistakes get the right 4xx status and a safe message in the standard envelope, never a 500. */
class ApiErrorHandlingIntegrationTest extends IntegrationTestSupport {

    private final LocalDate start = LocalDate.now().plusDays(5);
    private final LocalDate end = LocalDate.now().plusDays(6);

    // ---------- routing and protocol errors ----------

    // Anonymous callers get 401 for every non-public path before routing, so these use a signed-in user

    @Test
    void unknownUrl_is404() throws Exception {
        mockMvc.perform(get("/hotels/1/does-not-exist").header("Authorization", bearer(signup("GUEST"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Resource not found"));
    }

    @Test
    void wrongHttpMethod_is405() throws Exception {
        mockMvc.perform(delete("/bookings").header("Authorization", bearer(signup("GUEST"))))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.message").value("Method DELETE is not supported for this endpoint"));
    }

    @Test
    void unknownUrl_isUnauthorized_forAnonymousCallers() throws Exception {
        mockMvc.perform(get("/hotels/1/does-not-exist"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unsupportedContentType_is415() throws Exception {
        mockMvc.perform(withIp(post("/auth/login")).contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.message").value("Unsupported content type; send application/json"));
    }

    @Test
    void nonNumericPathVariable_is400() throws Exception {
        mockMvc.perform(get("/hotels/abc/info"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Invalid value for parameter 'hotelId'"));
    }

    @Test
    void invalidQueryParameterType_is400() throws Exception {
        mockMvc.perform(get("/hotels/1/info").param("startDate", "next-friday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Invalid value for parameter 'startDate'"));
    }

    // ---------- malformed bodies ----------

    @Test
    void malformedJson_is400() throws Exception {
        mockMvc.perform(post("/hotels/search").contentType(MediaType.APPLICATION_JSON).content("{\"city\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Malformed JSON in request body"));
    }

    @Test
    void missingBody_is400() throws Exception {
        mockMvc.perform(post("/hotels/search").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Request body is missing or malformed"));
    }

    @Test
    void invalidDateValue_namesTheField() throws Exception {
        mockMvc.perform(post("/hotels/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"tomorrow\",\"endDate\":\"" + end + "\",\"roomsCount\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Invalid value for field 'startDate'"))
                .andExpect(content().string(not(containsString("java."))));
    }

    @Test
    void unknownEnumValue_namesTheField() throws Exception {
        mockMvc.perform(withIp(post("/auth/signup")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"N\",\"email\":\"" + uniqueEmail() + "\",\"password\":\"pw123456\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Invalid value for field 'role'"));
    }

    @Test
    void invalidListElement_namesTheIndex() throws Exception {
        String guest = signup("GUEST");
        mockMvc.perform(post("/bookings/1/addGuests").header("Authorization", bearer(guest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"A\",\"age\":30},{\"name\":\"B\",\"age\":\"old\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Invalid value for field '[1].age'"));
    }

    // ---------- search validation ----------

    @Test
    void search_requiresDates_andAPositiveRoomCount() throws Exception {
        mockMvc.perform(post("/hotels/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\":\"Goa\",\"roomsCount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.subErrors", hasItem(containsString("startDate"))))
                .andExpect(jsonPath("$.error.subErrors", hasItem(containsString("endDate"))))
                .andExpect(jsonPath("$.error.subErrors", hasItem(containsString("roomsCount"))));
    }

    @Test
    void search_rejectsAnEndDateBeforeTheStartDate() throws Exception {
        mockMvc.perform(post("/hotels/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"" + end + "\",\"endDate\":\"" + start + "\",\"roomsCount\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("End date must be after start date"));
    }

    // ---------- auth validation ----------

    @Test
    void signup_validatesNameEmailAndPassword() throws Exception {
        mockMvc.perform(withIp(post("/auth/signup")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.subErrors", hasItem(containsString("name"))))
                .andExpect(jsonPath("$.error.subErrors", hasItem(containsString("email"))))
                .andExpect(jsonPath("$.error.subErrors", hasItem("password: must be between 8 and 72 characters")));
    }

    @Test
    void signup_rejectsPasswordsLongerThan72Bytes() throws Exception {
        String longPassword = "é".repeat(40); // 40 characters but 80 bytes in UTF-8
        mockMvc.perform(withIp(post("/auth/signup")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"N\",\"email\":\"" + uniqueEmail() + "\",\"password\":\"" + longPassword + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Password is too long"));
    }

    @Test
    void emails_areCaseInsensitive() throws Exception {
        String email = "Mixed.Case-" + System.nanoTime() + "@Test.Local";
        signup("GUEST", email);

        mockMvc.perform(withIp(post("/auth/login")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email.toLowerCase() + "\",\"password\":\"pw123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email.toLowerCase()));

        mockMvc.perform(withIp(post("/auth/signup")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"N\",\"email\":\"" + email.toUpperCase() + "\",\"password\":\"pw123456\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void login_requiresEmailAndPassword() throws Exception {
        mockMvc.perform(withIp(post("/auth/login")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.subErrors.length()").value(2));
    }

    // ---------- hotel and room validation ----------

    @Test
    void createHotel_requiresNameAndCity_andValidatesContactDetails() throws Exception {
        String manager = signup("HOTEL_MANAGER");
        mockMvc.perform(post("/admin/hotels").header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactInfo\":{\"email\":\"nope\"},\"photos\":[\"" + "x".repeat(300) + "\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.subErrors", hasItem(containsString("name"))))
                .andExpect(jsonPath("$.error.subErrors", hasItem(containsString("city"))))
                .andExpect(jsonPath("$.error.subErrors", hasItem(containsString("contactInfo.email"))))
                .andExpect(jsonPath("$.error.subErrors", hasItem(containsString("photos[0]"))));
    }

    @Test
    void updateHotel_allowsOmittedFields_butRejectsBlankOnes() throws Exception {
        String manager = signup("HOTEL_MANAGER");
        long hotelId = createHotel(manager);

        mockMvc.perform(put("/admin/hotels/" + hotelId).header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amenities\":[\"Spa\"]}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/admin/hotels/" + hotelId).header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRoom_validatesPriceCountAndCapacity() throws Exception {
        String manager = signup("HOTEL_MANAGER");
        long hotelId = createHotel(manager);

        mockMvc.perform(post("/admin/hotels/" + hotelId + "/rooms").header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"Suite\",\"basePrice\":-5,\"totalCount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.subErrors", hasItem(containsString("basePrice"))))
                .andExpect(jsonPath("$.error.subErrors", hasItem(containsString("totalCount"))))
                .andExpect(jsonPath("$.error.subErrors", hasItem(containsString("capacity"))));
    }

    // ---------- errors raised outside controllers ----------

    @Test
    void errorEndpoint_usesTheEnvelope_withoutDetails() throws Exception {
        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new RuntimeException("secret SQL detail")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("secret"))));
    }
}
