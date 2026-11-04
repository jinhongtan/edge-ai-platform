package com.edgeaiplatform.backend.config;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.build")
public record BuildProperties(Path directory, int maxWorkspaceMiB, boolean enabled, java.util.List<String> command, Duration timeout) { }
