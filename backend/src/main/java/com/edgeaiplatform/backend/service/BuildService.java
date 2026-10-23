package com.edgeaiplatform.backend.service;

import com.edgeaiplatform.backend.config.BuildProperties;
import com.edgeaiplatform.backend.dto.BuildJob;
import com.edgeaiplatform.backend.dto.BuildRequest;
import com.edgeaiplatform.backend.exception.InvalidBuildStateException;
import com.edgeaiplatform.backend.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BuildService {
    private final FileSystemModelStore models;
    private final FileSystemBuildStore builds;
    private final HardwareProfileService profiles;
    private final BuildProperties properties;

    public BuildService(FileSystemModelStore models, FileSystemBuildStore builds, HardwareProfileService profiles, BuildProperties properties) {
        this.models = models; this.builds = builds; this.profiles = profiles; this.properties = properties;
    }
    public BuildJob create(BuildRequest request) {
        if (!"FP32".equals(request.precision())) throw new IllegalArgumentException("Only FP32 builds are available in V1.");
        if (request.options().workspaceMiB() > properties.maxWorkspaceMiB()) throw new IllegalArgumentException("workspaceMiB exceeds the configured build limit.");
        var model = models.find(request.modelId()).orElseThrow(() -> new ResourceNotFoundException("Model was not found."));
        profiles.find(request.hardwareId());
        if (!"VALID".equals(model.validationStatus()) || model.inspection() == null) throw new InvalidBuildStateException("Model analysis must complete successfully before a build can be requested.");
        boolean dynamic = model.inspection().inputs().stream().anyMatch(input -> input.shape().stream().anyMatch(dimension -> dimension == null || dimension instanceof String || (dimension instanceof Number number && number.longValue() < 1)));
        if (dynamic) throw new InvalidBuildStateException("Dynamic input shapes are unsupported for the V1 static-shape build.");
        var compatibility = models.latestCompatibility(model.id()).orElseThrow(() -> new InvalidBuildStateException("Run a compatibility check before requesting a build."));
        if (!request.hardwareId().equals(compatibility.hardwareId())) throw new InvalidBuildStateException("Run compatibility for the selected hardware before requesting a build.");
        if ("INCOMPATIBLE".equals(compatibility.status())) throw new InvalidBuildStateException("Build is blocked by the compatibility result.");
        Instant now = Instant.now();
        String message = "Queued for a Jetson worker. A successful build does not verify runtime inference.";
        return builds.save(new BuildJob(UUID.randomUUID().toString(), model.id(), request.hardwareId(), "FP32", request.options().workspaceMiB(), model.digestSha256(), "QUEUED", "QUEUED", message, null, null, now, now));
    }
    public BuildJob get(String id) { return builds.find(id).orElseThrow(() -> new ResourceNotFoundException("Build was not found.")); }
    public List<BuildJob> listForModel(String modelId) {
        if (models.find(modelId).isEmpty()) throw new ResourceNotFoundException("Model was not found.");
        return builds.listForModel(modelId);
    }
}
