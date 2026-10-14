package com.stayease.ops;

import com.stayease.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OperationsIntegrationTest extends IntegrationTestSupport {

    @Test
    void healthEndpoints_arePublic_andRevealNoDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist()); // not wrapped in the ApiResponse envelope
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    void otherActuatorEndpoints_areNotExposed() throws Exception {
        String token = signup("HOTEL_MANAGER");
        for (String endpoint : new String[]{"/actuator/env", "/actuator/beans", "/actuator/configprops"}) {
            mockMvc.perform(get(endpoint).header("Authorization", bearer(token)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void cors_allowsTheConfiguredOrigin_only() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));

        mockMvc.perform(options("/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
