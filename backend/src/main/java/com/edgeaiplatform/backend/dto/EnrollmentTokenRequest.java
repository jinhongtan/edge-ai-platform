package com.edgeaiplatform.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record EnrollmentTokenRequest(@NotBlank String tenantId) { }
