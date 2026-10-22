package com.edgeaiplatform.backend.service;

import com.edgeaiplatform.backend.dto.ModelSummary;
import com.edgeaiplatform.backend.exception.UploadRejectedException;
import com.edgeaiplatform.backend.model.ModelInspection;
import com.edgeaiplatform.backend.service.FileSystemModelStore.StoredUpload;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ModelService {
    private final FileSystemModelStore store;
    private final ModelInspector inspector;

    public ModelService(FileSystemModelStore store, ModelInspector inspector) {
        this.store = store;
        this.inspector = inspector;
    }

    public List<ModelSummary> list() {
        return store.list();
    }

    public ModelSummary upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UploadRejectedException("EMPTY_FILE", "Choose an ONNX model file to upload.");
        }
        String fileName = originalFileName(file.getOriginalFilename());
        String id = store.newId();
        StoredUpload stored;
        try {
            stored = store.stageUpload(id, file.getInputStream());
        } catch (UploadRejectedException ex) {
            throw ex;
        } catch (IOException ex) {
            store.deleteQuietly(id);
            throw new UploadRejectedException("FILE_UNREADABLE", "Model file could not be read.");
        }
        try {
            ModelInspection inspection = inspector.inspect(stored.file());
            return store.save(new ModelSummary(id, fileName, stored.digestSha256(), Instant.now(),
                "VALID", null, null, inspection));
        } catch (ModelInspectionException ex) {
            return saveFailedInspection(id, fileName, stored, ex);
        } catch (IOException ex) {
            store.deleteQuietly(id);
            throw new UploadRejectedException("FILE_UNREADABLE", "Model file could not be stored.");
        }
    }

    private ModelSummary saveFailedInspection(String id, String fileName, StoredUpload stored,
                                               ModelInspectionException error) {
        try {
            return store.save(new ModelSummary(id, fileName, stored.digestSha256(), Instant.now(),
                "INVALID", error.code(), error.getMessage(), null));
        } catch (IOException ex) {
            store.deleteQuietly(id);
            throw new UploadRejectedException("FILE_UNREADABLE", "Model file could not be stored.");
        }
    }

    static String originalFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new UploadRejectedException("UNSUPPORTED_FILE_TYPE", "Only ONNX model files are supported.");
        }
        String normalized = originalFilename.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (name.isBlank() || !name.toLowerCase(Locale.ROOT).endsWith(".onnx") || name.length() == 5) {
            throw new UploadRejectedException("UNSUPPORTED_FILE_TYPE", "Only ONNX model files are supported.");
        }
        if (name.contains("..")) {
            throw new UploadRejectedException("UNSUPPORTED_FILE_TYPE", "Only ONNX model files are supported.");
        }
        return name;
    }
}
