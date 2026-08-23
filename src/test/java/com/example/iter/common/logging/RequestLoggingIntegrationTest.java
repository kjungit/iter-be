package com.example.iter.common.logging;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class RequestLoggingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requestLoggingFilterIsRegisteredForApiRequests() throws Exception {
        mockMvc.perform(get("/api/v1/devices")
                        .header(RequestLoggingFilter.REQUEST_ID_HEADER, "integration-request-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestLoggingFilter.REQUEST_ID_HEADER, "integration-request-1"));
    }
}
