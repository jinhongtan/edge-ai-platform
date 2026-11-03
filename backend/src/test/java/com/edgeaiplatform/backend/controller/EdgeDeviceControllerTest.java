package com.edgeaiplatform.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edgeaiplatform.backend.dto.EnrollmentTokenResponse;
import com.edgeaiplatform.backend.service.EdgeEnrollmentService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EdgeDeviceController.class)
class EdgeDeviceControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean EdgeEnrollmentService service;

    @Test void issuesEnrollmentTokenWithAdminKey() throws Exception {
        when(service.issue("admin", "tenant-a")).thenReturn(new EnrollmentTokenResponse("returned-once", Instant.parse("2026-11-01T00:00:00Z")));
        mvc.perform(post("/api/v1/edge-devices/enrollment-tokens").header("X-Edge-Admin-Key", "admin")
                .contentType(MediaType.APPLICATION_JSON).content("{\"tenantId\":\"tenant-a\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.token").value("returned-once"));
    }
}
