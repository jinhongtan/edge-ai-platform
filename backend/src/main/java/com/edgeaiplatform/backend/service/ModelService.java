package com.edgeaiplatform.backend.service;

import com.edgeaiplatform.backend.config.UploadProperties;
import com.edgeaiplatform.backend.dto.ModelSummary;
import com.edgeaiplatform.backend.model.ModelInspection;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ModelService {
    private final UploadProperties uploadProperties;
    private final ModelInspector modelInspector;
    private final Map<String, ModelSummary> models = new ConcurrentHashMap<>();

    public ModelService(UploadProperties uploadProperties, ModelInspector modelInspector) {
        this.uploadProperties = uploadProperties;
        this.modelInspector = modelInspector;
    }

    public List<ModelSummary> models() {
        return models.values().stream()
                .sorted(Comparator.comparing(ModelSummary::uploadedAt).reversed())
                .toList();
    }

    public ModelSummary upload(MultipartFile file) throws ModelInspectionException {
        if (file == null || file.isEmpty()) {
            throw new ModelInspectionException("EMPTY_FILE", "Model file is empty.");
        }
        String filename = displayName(file.getOriginalFilename());
        if (file.getSize() > uploadProperties.maxBytes()) {
            throw new ModelInspectionException("FILE_TOO_LARGE",
                    "Model exceeds the " + uploadProperties.maxBytes() + "-byte size limit.");
        }
        String id = UUID.randomUUID().toString();
        Path stored = null;
        try {
            Path directory = Files.createDirectories(uploadProperties.storageDir());
            stored = directory.resolve(id + ".onnx");
            file.transferTo(stored);
            String digest = sha256(stored);
            ModelInspection inspection = modelInspector.inspect(stored);
            ModelSummary summary = new ModelSummary(id, filename, digest, Instant.now(), inspection);
            models.put(id, summary);
            return summary;
        } catch (ModelInspectionException ex) {
            deleteQuietly(stored);
            throw ex;
        } catch (IOException ex) {
            deleteQuietly(stored);
            throw new ModelInspectionException("FILE_UNREADABLE", "Model file could not be stored.");
        }
    }

    private static String displayName(String originalFilename) throws ModelInspectionException {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ModelInspectionException("UNSUPPORTED_FILE_TYPE", "Only ONNX model files are supported.");
        }
        String filename = Path.of(originalFilename).getFileName().toString();
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".onnx")) {
            throw new ModelInspectionException("UNSUPPORTED_FILE_TYPE", "Only ONNX model files are supported.");
        }
        return filename;
    }

    private static String sha256(Path file) throws ModelInspectionException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new ModelInspectionException("FILE_UNREADABLE", "Model file could not be read.");
        }
    }

    private static void deleteQuietly(Path stored) {
        if (stored == null) {
            return;
        }
        try {
            Files.deleteIfExists(stored);
        } catch (IOException ignored) {
            // Failed uploads must not keep untrusted files; ignore cleanup races.
        }
    }
}
