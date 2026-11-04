package com.edgeaiplatform.backend.service;

import com.edgeaiplatform.backend.config.BuildProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs at most one local build at a time. Enable only on the Jetson that has TensorRT installed. */
@Component
public class LocalBuildDispatcher {
    private final BuildProperties properties; private final FileSystemBuildStore builds; private final FileSystemModelStore models;
    private boolean running;
    public LocalBuildDispatcher(BuildProperties properties, FileSystemBuildStore builds, FileSystemModelStore models) { this.properties = properties; this.builds = builds; this.models = models; }
    @Scheduled(fixedDelayString = "${app.build.poll-interval:2000}")
    public synchronized void dispatchOne() {
        if (!properties.enabled() || running) return;
        var claimed = builds.claimNextQueued();
        if (claimed.isEmpty()) return;
        running = true;
        try { execute(claimed.get()); } finally { running = false; }
    }
    private void execute(com.edgeaiplatform.backend.dto.BuildJob job) {
        Path directory = builds.jobDirectory(job.id()); Path engine = directory.resolve("model.engine"); Path log = directory.resolve("build.log");
        try {
            Files.createDirectories(directory);
            Path sourceModel = models.modelPath(job.modelId());
            Path model = directory.resolve("model.onnx");
            Files.copy(sourceModel, model, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            var command = new ArrayList<>(properties.command());
            command.add(model.toAbsolutePath().toString()); command.add(engine.toAbsolutePath().toString()); command.add(Integer.toString(job.workspaceMiB()));
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Files.writeString(log, output, StandardCharsets.UTF_8);
            if (!finished) { process.destroyForcibly(); builds.update(job, "FAILED", "FAILED", "Local TensorRT build timed out.", null, "build.log"); return; }
            if (process.exitValue() != 0 || !Files.isRegularFile(engine) || Files.size(engine) == 0) { builds.update(job, "FAILED", "FAILED", "Local TensorRT build failed. Review the bounded local build log.", null, "build.log"); return; }
            builds.update(job, "COMPLETED", "COMPLETED", "Engine created locally. Runtime inference has not been verified.", "model.engine", "build.log");
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            builds.update(job, "FAILED", "FAILED", "Local TensorRT build could not be started.", null, "build.log");
        }
    }
}
