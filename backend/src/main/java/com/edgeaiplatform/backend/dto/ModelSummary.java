package com.edgeaiplatform.backend.dto;

import com.edgeaiplatform.backend.model.ModelInspection;
import java.time.Instant;

public record ModelSummary(String id, String filename, String digestSha256, Instant uploadedAt, ModelInspection inspection) { }
