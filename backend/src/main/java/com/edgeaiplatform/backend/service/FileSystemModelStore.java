package com.edgeaiplatform.backend.service;

import com.edgeaiplatform.backend.config.StorageProperties;
import com.edgeaiplatform.backend.dto.ModelSummary;
import com.edgeaiplatform.backend.dto.CompatibilityResult;
import com.edgeaiplatform.backend.exception.UploadRejectedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class FileSystemModelStore {
    private static final String MODEL_FILE = "model.onnx";
    private static final String METADATA_FILE = "metadata.json";
    private static final String COMPATIBILITY_FILE = "compatibility.json";

    private final Path root;
    private final long maxBytes;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, ModelSummary> models = new ConcurrentHashMap<>();

    public FileSystemModelStore(StorageProperties storageProperties, ObjectMapper objectMapper) {
        this.root = storageProperties.directory();
        this.maxBytes = storageProperties.maxBytes();
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadExisting() throws IOException {
        Files.createDirectories(root);
        try (var directories = Files.list(root)) {
            directories.filter(Files::isDirectory).forEach(this::loadDirectory);
        }
    }

    public Path modelPath(String id) {
        return root.resolve(id).resolve(MODEL_FILE);
    }

    public StoredUpload stageUpload(String id, InputStream content) throws IOException {
        Path directory = root.resolve(id);
        Files.createDirectories(directory);
        Path file = directory.resolve(MODEL_FILE);
        MessageDigest digest = sha256();
        long size = 0;
        boolean tooLarge = false;
        try (OutputStream output = new DigestOutputStream(Files.newOutputStream(file), digest)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = content.read(buffer)) != -1) {
                size += read;
                if (size > maxBytes) {
                    tooLarge = true;
                    break;
                }
                output.write(buffer, 0, read);
            }
        } catch (IOException ex) {
            deleteQuietly(id);
            throw ex;
        }
        if (tooLarge) {
            deleteQuietly(id);
            throw new UploadRejectedException("FILE_TOO_LARGE",
                "Model exceeds the " + maxBytes + "-byte size limit.");
        }
        if (size == 0) {
            deleteQuietly(id);
            throw new UploadRejectedException("EMPTY_FILE", "Model file is empty.");
        }
        return new StoredUpload(id, file, size, HexFormat.of().formatHex(digest.digest()));
    }

    public ModelSummary save(ModelSummary summary) throws IOException {
        objectMapper.writeValue(root.resolve(summary.id()).resolve(METADATA_FILE).toFile(), summary);
        models.put(summary.id(), summary);
        return summary;
    }

    public CompatibilityResult saveCompatibility(CompatibilityResult result) throws IOException {
        objectMapper.writeValue(root.resolve(result.modelId()).resolve(COMPATIBILITY_FILE).toFile(), result);
        return result;
    }

    public Optional<CompatibilityResult> latestCompatibility(String modelId) {
        Path file = root.resolve(modelId).resolve(COMPATIBILITY_FILE);
        try { return Files.isRegularFile(file) ? Optional.of(objectMapper.readValue(file.toFile(), CompatibilityResult.class)) : Optional.empty(); }
        catch (IOException ex) { return Optional.empty(); }
    }

    public void deleteQuietly(String id) {
        models.remove(id);
        Path directory = root.resolve(id);
        try {
            Path metadata = directory.resolve(METADATA_FILE);
            Path model = directory.resolve(MODEL_FILE);
            Files.deleteIfExists(metadata);
            Files.deleteIfExists(model);
            Files.deleteIfExists(directory);
        } catch (IOException ignored) {
            // Controlled cleanup; callers already have an actionable error.
        }
    }

    public List<ModelSummary> list() {
        var copy = new ArrayList<>(models.values());
        copy.sort(Comparator.comparing(ModelSummary::uploadedAt).reversed());
        return List.copyOf(copy);
    }

    public Optional<ModelSummary> find(String id) {
        return Optional.ofNullable(models.get(id));
    }

    public String newId() {
        return UUID.randomUUID().toString();
    }

    private void loadDirectory(Path directory) {
        Path metadata = directory.resolve(METADATA_FILE);
        if (!Files.isRegularFile(metadata)) {
            return;
        }
        try {
            ModelSummary summary = objectMapper.readValue(metadata.toFile(), ModelSummary.class);
            if (summary.id() != null) {
                models.put(summary.id(), summary);
            }
        } catch (IOException ignored) {
            // Skip unreadable metadata so startup is not blocked by a single corrupt record.
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }

    public record StoredUpload(String id, Path file, long sizeBytes, String digestSha256) { }
}
