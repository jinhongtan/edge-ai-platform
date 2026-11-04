package com.edgeaiplatform.backend.dto;

import java.time.Instant;
import java.util.List;

public record CompatibilityResult(String id, String modelId, String hardwareId, String status,
                                  List<UnsupportedOperator> unsupportedOperators, List<String> warnings,
                                  List<String> supportedPrecisions, Instant createdAt) { }
