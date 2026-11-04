package com.edgeaiplatform.backend.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record AgentEnrollmentRequest(@NotBlank String token, @NotBlank String displayName, String agentVersion,
                                     Map<String, Object> capabilities) { }
