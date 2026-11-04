package com.edgeaiplatform.backend.dto;

import java.util.Map;

public record AgentHeartbeatRequest(Map<String, Object> capabilities, Long freeDiskBytes, String activity) { }
