package com.edgeaiplatform.backend.controller;

import com.edgeaiplatform.backend.dto.ApiResponse;
import com.edgeaiplatform.backend.dto.BuildJob;
import com.edgeaiplatform.backend.dto.BuildRequest;
import com.edgeaiplatform.backend.service.BuildService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class BuildController {
    private final BuildService service;
    public BuildController(BuildService service) { this.service = service; }
    @PostMapping("/builds") @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<BuildJob> create(@Valid @RequestBody BuildRequest request) { return new ApiResponse<>(service.create(request)); }
    @GetMapping("/builds/{buildId}") public ApiResponse<BuildJob> get(@PathVariable String buildId) { return new ApiResponse<>(service.get(buildId)); }
    @GetMapping("/models/{modelId}/builds") public ApiResponse<List<BuildJob>> list(@PathVariable String modelId) { return new ApiResponse<>(service.listForModel(modelId)); }
}
