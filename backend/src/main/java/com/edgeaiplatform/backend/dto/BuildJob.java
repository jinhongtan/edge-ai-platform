package com.edgeaiplatform.backend.dto;

import java.time.Instant;

/** A persisted request only; COMPLETED is reserved for a worker-verified engine artifact. */
public record BuildJob(
    String id, String modelId, String hardwareId, String precision, int workspaceMiB,
    String modelDigestSha256, String status, String stage, String message,
    String artifactFileName, String logFileName, Instant createdAt, Instant updatedAt
) { }
