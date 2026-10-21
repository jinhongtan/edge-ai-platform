package com.edgeaiplatform.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.edgeaiplatform.backend.config.UploadProperties;
import com.edgeaiplatform.backend.model.ModelInspection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ModelServiceTest {
    @TempDir Path tempDir;

    @Test void storesInspectedOnnxAndListsIt() throws Exception {
        ModelInspection inspection = new ModelInspection(4, 9, List.of(), List.of(), List.of());
        ModelService service = new ModelService(new UploadProperties(1024, tempDir), path -> inspection);
        var created = service.upload(new MockMultipartFile("file", "tiny.onnx", "application/octet-stream", new byte[] {1, 2, 3, 4}));
        assertEquals("tiny.onnx", created.filename());
        assertEquals(64, created.digestSha256().length());
        assertEquals(1, service.models().size());
        assertTrue(Files.exists(tempDir.resolve(created.id() + ".onnx")));
    }

    @Test void rejectsNonOnnxFilenames() {
        ModelService service = new ModelService(new UploadProperties(1024, tempDir), path -> {
            throw new AssertionError("inspector should not run");
        });
        var error = assertThrows(ModelInspectionException.class, () ->
                service.upload(new MockMultipartFile("file", "notes.txt", "text/plain", new byte[] {1})));
        assertEquals("UNSUPPORTED_FILE_TYPE", error.code());
        assertEquals(0, service.models().size());
    }
}
