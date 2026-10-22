package com.edgeaiplatform.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CompatibilityRequest(@NotBlank String hardwareId) { }
