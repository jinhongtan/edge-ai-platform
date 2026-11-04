package com.edgeaiplatform.backend.controller;

import com.edgeaiplatform.backend.dto.*;
import com.edgeaiplatform.backend.service.EdgeEnrollmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class EdgeDeviceController {
    private final EdgeEnrollmentService service;
    public EdgeDeviceController(EdgeEnrollmentService service) { this.service = service; }
    @PostMapping("/api/v1/edge-devices/enrollment-tokens") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EnrollmentTokenResponse> issue(@RequestHeader(value = "X-Edge-Admin-Key", required = false) String key, @Valid @RequestBody EnrollmentTokenRequest request) { return new ApiResponse<>(service.issue(key, request.tenantId())); }
    @GetMapping("/api/v1/edge-devices") public ApiResponse<List<EdgeDevice>> list(@RequestHeader(value = "X-Edge-Admin-Key", required = false) String key, @RequestParam String tenantId) { return new ApiResponse<>(service.list(key, tenantId)); }
    @PostMapping("/api/v1/edge-devices/{id}/approve") public ApiResponse<EdgeDevice> approve(@RequestHeader(value = "X-Edge-Admin-Key", required = false) String key, @PathVariable String id) { return new ApiResponse<>(service.approve(key, id)); }
    @PostMapping("/api/agent/v1/enroll") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<AgentEnrollmentResponse> enroll(@Valid @RequestBody AgentEnrollmentRequest request) { return new ApiResponse<>(service.enroll(request)); }
    @PostMapping("/api/agent/v1/heartbeat") public ApiResponse<EdgeDevice> heartbeat(@RequestHeader(value = "Authorization", required = false) String auth, @RequestBody AgentHeartbeatRequest request) { return new ApiResponse<>(service.heartbeat(bearer(auth), request)); }
    private static String bearer(String value) { if (value == null || !value.startsWith("Bearer ")) throw new com.edgeaiplatform.backend.exception.AgentAccessException(401, "INVALID_DEVICE_CREDENTIAL", "Bearer device credential is required."); return value.substring(7); }
}
