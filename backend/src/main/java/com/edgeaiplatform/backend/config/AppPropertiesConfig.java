package com.edgeaiplatform.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({UploadProperties.class, WorkerProperties.class})
public class AppPropertiesConfig { }
