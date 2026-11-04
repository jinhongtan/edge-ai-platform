package com.edgeaiplatform.backend.dto;

import java.time.Instant;

public record AuthSession(String accessToken, Instant expiresAt, UserIdentity user) { }
