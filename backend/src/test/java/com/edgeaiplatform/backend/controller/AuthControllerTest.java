package com.edgeaiplatform.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edgeaiplatform.backend.dto.AuthSession;
import com.edgeaiplatform.backend.dto.UserIdentity;
import com.edgeaiplatform.backend.service.AuthService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthService service;

    @Test void registersAndReturnsAnAccessToken() throws Exception {
        var session = new AuthSession("opaque-token", Instant.parse("2026-11-03T12:00:00Z"), new UserIdentity("user-1", "user@example.com", "Acme"));
        when(service.register(any())).thenReturn(session);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"organization\":\"Acme\",\"password\":\"long-enough-password\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.accessToken").value("opaque-token"));
    }
}
