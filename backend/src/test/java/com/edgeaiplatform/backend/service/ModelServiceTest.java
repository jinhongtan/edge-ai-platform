package com.edgeaiplatform.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.edgeaiplatform.backend.config.StorageProperties;
import com.edgeaiplatform.backend.exception.UploadRejectedException;
import com.edgeaiplatform.backend.model.ModelInspection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ModelServiceTest {
    @TempDir Path tempDir;
    private FileSystemModelStore store;
    private ModelService service;
    private Path inspected;

    @BeforeEach void setup() throws Exception {
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        store = new FileSystemModelStore(new StorageProperties(tempDir, 1024), mapper);
        store.loadExisting();
        ModelInspector inspector = path -> {
            inspected = path;
            try {
                return new ModelInspection(Files.size(path), 9, List.of(), List.of(), List.of(), List.of(), java.util.Map.of(), 0);
            } catch (java.io.IOException ex) {
                throw new IllegalStateException(ex);
            }
        };
        service = new ModelService(store, inspector);
    }

    @Test void storesValidOnnxAndListsIt() throws Exception {
        var file = new MockMultipartFile("file", "tiny.onnx", "application/octet-stream", new byte[]{1, 2, 3, 4});
        var saved = service.upload(file);
        assertEquals("tiny.onnx", saved.fileName());
        assertEquals(64, saved.digestSha256().length());
        assertTrue(Files.isRegularFile(inspected));
        assertFalse(inspected.toString().contains("tiny.onnx"));
        assertEquals(1, service.list().size());
        assertTrue(Files.isRegularFile(tempDir.resolve(saved.id()).resolve("metadata.json")));
    }

    @Test void rejectsNonOnnxNames() {
        var file = new MockMultipartFile("file", "notes.txt", "text/plain", new byte[]{1});
        var error = assertThrows(UploadRejectedException.class, () -> service.upload(file));
        assertEquals("UNSUPPORTED_FILE_TYPE", error.code());
        assertEquals(0, service.list().size());
    }

    @Test void rejectsFilesOverLimitAndCleansUp() {
        var oversized = new MockMultipartFile("file", "big.onnx", "application/octet-stream", new byte[2048]);
        var error = assertThrows(UploadRejectedException.class, () -> service.upload(oversized));
        assertEquals("FILE_TOO_LARGE", error.code());
        assertEquals(0, service.list().size());
        assertEquals(0, tempDir.toFile().listFiles() == null ? 0 : tempDir.toFile().listFiles().length);
    }

    @Test void storesFileWhenInspectionFails() throws Exception {
        service = new ModelService(store, path -> { throw new ModelInspectionException("INVALID_ONNX", "File is not a valid ONNX model. Verify the file and export it again."); });
        var file = new MockMultipartFile("file", "broken.onnx", "application/octet-stream", new byte[]{1, 2, 3});
        var saved = service.upload(file);
        assertEquals("INVALID", saved.validationStatus());
        assertEquals("INVALID_ONNX", saved.validationCode());
        assertEquals(1, service.list().size());
        assertTrue(Files.isRegularFile(tempDir.resolve(saved.id()).resolve("model.onnx")));
        assertTrue(Files.isRegularFile(tempDir.resolve(saved.id()).resolve("metadata.json")));
    }
}
