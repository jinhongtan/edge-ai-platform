package com.edgeaiplatform.backend.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(@DefaultValue("104857600") long maxBytes, Path storageDir) { }
