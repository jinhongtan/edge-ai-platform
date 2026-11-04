package com.edgeaiplatform.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.edgeaiplatform.backend.dto.ModelSummary;
import com.edgeaiplatform.backend.exception.UploadRejectedException;
import com.edgeaiplatform.backend.model.ModelInspection;
import com.edgeaiplatform.backend.service.CatalogService;
import com.edgeaiplatform.backend.service.ModelService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ModelController.class, CatalogController.class, HealthController.class})
class ModelControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ModelService modelService;
    @MockitoBean CatalogService catalogService;

    @Test void uploadReturnsCreatedModel() throws Exception {
        var summary = new ModelSummary("id-1", "tiny.onnx", "abc", Instant.parse("2026-01-01T00:00:00Z"),
            "VALID", null, null, new ModelInspection(12, 9, List.of(), List.of(), List.of(), List.of(), java.util.Map.of(), 0));
        when(modelService.upload(any())).thenReturn(summary);
        mvc.perform(multipart("/api/v1/models").file(new MockMultipartFile("file", "tiny.onnx", "application/octet-stream", new byte[]{1, 2, 3})))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.fileName").value("tiny.onnx"))
            .andExpect(jsonPath("$.data.id").value("id-1"));
    }

    @Test void invalidTypeReturnsStructuredError() throws Exception {
        when(modelService.upload(any())).thenThrow(new UploadRejectedException("UNSUPPORTED_FILE_TYPE", "Only ONNX model files are supported."));
        mvc.perform(multipart("/api/v1/models").file(new MockMultipartFile("file", "notes.txt", "text/plain", new byte[]{1})))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
    }

    @Test void invalidOnnxIsStoredWithValidationResult() throws Exception {
        var summary = new ModelSummary("id-2", "broken.onnx", "abc", Instant.parse("2026-01-01T00:00:00Z"),
            "INVALID", "INVALID_ONNX", "File is not a valid ONNX model. Verify the file and export it again.", null);
        when(modelService.upload(any())).thenReturn(summary);
        mvc.perform(multipart("/api/v1/models").file(new MockMultipartFile("file", "broken.onnx", "application/octet-stream", new byte[]{1})))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.validationStatus").value("INVALID"))
            .andExpect(jsonPath("$.data.validationCode").value("INVALID_ONNX"));
    }
}
