package com.edgeaiplatform.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.edgeaiplatform.backend.service.CatalogService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({HealthController.class, CatalogController.class})
class ApiControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CatalogService catalogService;

    @Test void healthRespondsUp() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
    @Test void listEndpointsReturnEmptyArrays() throws Exception {
        when(catalogService.models()).thenReturn(List.of());
        when(catalogService.hardware()).thenReturn(List.of());
        when(catalogService.deployments()).thenReturn(List.of());
        mvc.perform(get("/api/v1/models")).andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/v1/hardware")).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/v1/deployments")).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }
}
