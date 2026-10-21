package com.edgeaiplatform.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.worker")
public record WorkerProperties(
        @DefaultValue("python") String python,
        @DefaultValue("onnx_worker") String module,
        @DefaultValue("30") int timeoutSeconds,
        @DefaultValue("1048576") int maxOutputBytes,
        String workingDirectory) {
    public WorkerProperties {
        if (workingDirectory != null && workingDirectory.isBlank()) {
            workingDirectory = null;
        }
    }
}
