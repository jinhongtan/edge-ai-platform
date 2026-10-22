package com.edgeaiplatform.backend.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.inspector")
public record InspectorProperties(List<String> command, Duration timeout, int maxOutputBytes) { }
