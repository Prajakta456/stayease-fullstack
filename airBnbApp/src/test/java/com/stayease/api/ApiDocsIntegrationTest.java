package com.stayease.api;

import com.stayease.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiDocsIntegrationTest extends IntegrationTestSupport {

    @Test
    void openApiSpec_isPublic_unwrapped_andDocumentsTheEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("StayEase Hotel Booking API"))
                .andExpect(jsonPath("$.data").doesNotExist()) // not wrapped in the ApiResponse envelope
                .andExpect(jsonPath("$.paths['/bookings/{bookingId}/payments'].post").exists())
                .andExpect(jsonPath("$.paths['/admin/hotels/{hotelId}/deactivate'].patch").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void swaggerUi_isPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
