package com.edgeaiplatform.backend.config;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Local-development adapter settings. Production must use the database-backed implementation. */
@ConfigurationProperties(prefix = "app.edge-agent")
public record EdgeAgentProperties(boolean enabled, Path directory, String adminKey, Duration enrollmentTokenTtl) { }
