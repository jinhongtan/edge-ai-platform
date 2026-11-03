package com.edgeaiplatform.backend.dto;

import java.time.Instant;
import java.util.Map;

public record EdgeDevice(String id, String tenantId, String displayName, String status, String agentVersion,
                         Map<String, Object> capabilities, Instant lastHeartbeatAt, Instant createdAt, Instant updatedAt) { }
