package com.edgeaiplatform.backend.service;

import com.edgeaiplatform.backend.config.BuildProperties;
import com.edgeaiplatform.backend.dto.BuildJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/** Development persistence for build requests. Production must replace this with PostgreSQL plus an outbox. */
@Component
public class FileSystemBuildStore {
    private final Path root;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, BuildJob> jobs = new ConcurrentHashMap<>();

    public FileSystemBuildStore(BuildProperties properties, ObjectMapper objectMapper) {
        this.root = properties.directory();
        this.objectMapper = objectMapper;
    }
    @PostConstruct void loadExisting() throws IOException {
        Files.createDirectories(root);
        try (var files = Files.list(root)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                try { BuildJob job = objectMapper.readValue(path.toFile(), BuildJob.class); jobs.put(job.id(), job); }
                catch (IOException ignored) { /* A corrupt development record must not block startup. */ }
            });
        }
    }
    public BuildJob save(BuildJob job) {
        try {
            objectMapper.writeValue(root.resolve(job.id() + ".json").toFile(), job);
            jobs.put(job.id(), job);
            return job;
        } catch (IOException exception) { throw new IllegalStateException("Build request could not be stored."); }
    }
    public synchronized Optional<BuildJob> claimNextQueued() {
        return jobs.values().stream().filter(job -> "QUEUED".equals(job.status()))
            .min(Comparator.comparing(BuildJob::createdAt)).map(job -> save(replace(job, "BUILDING", "BUILDING", "Preparing local TensorRT build.")));
    }
    public BuildJob update(BuildJob job, String status, String stage, String message, String artifactFileName, String logFileName) {
        return save(new BuildJob(job.id(), job.modelId(), job.hardwareId(), job.precision(), job.workspaceMiB(), job.modelDigestSha256(), status, stage, message, artifactFileName, logFileName, job.createdAt(), java.time.Instant.now()));
    }
    public Path jobDirectory(String id) { return root.resolve(id); }
    public Optional<BuildJob> find(String id) { return Optional.ofNullable(jobs.get(id)); }
    public List<BuildJob> listForModel(String modelId) {
        var result = new ArrayList<>(jobs.values().stream().filter(job -> job.modelId().equals(modelId)).toList());
        result.sort(Comparator.comparing(BuildJob::createdAt).reversed());
        return List.copyOf(result);
    }
    private static BuildJob replace(BuildJob job, String status, String stage, String message) {
        return new BuildJob(job.id(), job.modelId(), job.hardwareId(), job.precision(), job.workspaceMiB(), job.modelDigestSha256(), status, stage, message, job.artifactFileName(), job.logFileName(), job.createdAt(), java.time.Instant.now());
    }
}
