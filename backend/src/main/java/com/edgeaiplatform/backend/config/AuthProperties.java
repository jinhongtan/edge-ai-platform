package com.edgeaiplatform.backend.config;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Development authentication settings; production must use a durable identity provider/store. */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(Path directory, Duration sessionTtl) { }
