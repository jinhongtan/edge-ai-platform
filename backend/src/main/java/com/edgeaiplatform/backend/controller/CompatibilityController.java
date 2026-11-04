package com.edgeaiplatform.backend.controller;

import com.edgeaiplatform.backend.dto.*;
import com.edgeaiplatform.backend.service.CompatibilityService;
import com.edgeaiplatform.backend.service.HardwareProfileService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CompatibilityController {
    private final HardwareProfileService profiles; private final CompatibilityService compatibility;
    public CompatibilityController(HardwareProfileService profiles, CompatibilityService compatibility) { this.profiles = profiles; this.compatibility = compatibility; }
    @GetMapping("/hardware") public ApiResponse<List<HardwareProfile>> hardware() { return new ApiResponse<>(profiles.list()); }
    @GetMapping("/hardware/{hardwareId}") public ApiResponse<HardwareProfile> hardware(@PathVariable String hardwareId) { return new ApiResponse<>(profiles.find(hardwareId)); }
    @PostMapping("/models/{modelId}/compatibility-check") public ApiResponse<CompatibilityResult> check(@PathVariable String modelId, @Valid @RequestBody CompatibilityRequest request) { return new ApiResponse<>(compatibility.check(modelId, request.hardwareId())); }
    @GetMapping("/models/{modelId}/compatibility") public ApiResponse<CompatibilityResult> latest(@PathVariable String modelId) { return new ApiResponse<>(compatibility.latest(modelId)); }
}
