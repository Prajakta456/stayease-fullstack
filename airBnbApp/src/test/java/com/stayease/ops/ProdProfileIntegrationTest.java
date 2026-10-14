package com.stayease.ops;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The prod profile on top of the in-memory test database: API docs are hidden by default. */
@SpringBootTest(properties = "jwt.secret=test-secret-that-is-at-least-32-bytes-long!")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "prod"})
class ProdProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocs_areDisabledByDefault() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }

    @Test
    void healthCheck_stillWorks() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
