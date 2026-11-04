package com.edgeaiplatform.backend.dto;

import com.edgeaiplatform.backend.model.ModelInspection;
import java.time.Instant;

public record ModelSummary(String id, String fileName, String digestSha256, Instant uploadedAt,
                           String validationStatus, String validationCode, String validationMessage,
                           ModelInspection inspection) { }
