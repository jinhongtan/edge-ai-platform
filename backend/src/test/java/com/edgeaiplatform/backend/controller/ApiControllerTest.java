package com.edgeaiplatform.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.edgeaiplatform.backend.dto.ModelSummary;
import com.edgeaiplatform.backend.model.ModelInspection;
import com.edgeaiplatform.backend.service.CatalogService;
import com.edgeaiplatform.backend.service.ModelInspectionException;
import com.edgeaiplatform.backend.service.ModelService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({HealthController.class, CatalogController.class})
class ApiControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CatalogService catalogService;
    @MockitoBean ModelService modelService;

    @Test void healthRespondsUp() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test void compatibilityReportsReturnsEmptyArray() throws Exception {
        when(catalogService.compatibilityReports()).thenReturn(List.of());
        mvc.perform(get("/api/v1/compatibility-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test void listEndpointsReturnEmptyArrays() throws Exception {
        when(modelService.models()).thenReturn(List.of());
        when(catalogService.hardware()).thenReturn(List.of());
        when(catalogService.deployments()).thenReturn(List.of());
        mvc.perform(get("/api/v1/models")).andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/v1/hardware")).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/v1/deployments")).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }

    @Test void uploadReturnsCreatedModel() throws Exception {
        var summary = new ModelSummary("id-1", "tiny.onnx", "abc", Instant.parse("2026-01-01T00:00:00Z"),
                new ModelInspection(4, 9, List.of(), List.of(), List.of()));
        when(modelService.upload(any())).thenReturn(summary);
        mvc.perform(multipart("/api/v1/models").file(new MockMultipartFile("file", "tiny.onnx", "application/octet-stream", new byte[] {1, 2, 3, 4})))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.filename").value("tiny.onnx"))
                .andExpect(jsonPath("$.data.id").value("id-1"));
    }

    @Test void uploadMapsInspectionErrors() throws Exception {
        when(modelService.upload(any())).thenThrow(new ModelInspectionException("INVALID_ONNX", "File is not a valid ONNX model. Verify the file and export it again."));
        mvc.perform(multipart("/api/v1/models").file(new MockMultipartFile("file", "tiny.onnx", "application/octet-stream", new byte[] {1})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ONNX"));
    }
}
