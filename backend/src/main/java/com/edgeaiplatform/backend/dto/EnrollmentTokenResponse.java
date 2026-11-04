package com.edgeaiplatform.backend.dto;

import java.time.Instant;

public record EnrollmentTokenResponse(String token, Instant expiresAt) { }
