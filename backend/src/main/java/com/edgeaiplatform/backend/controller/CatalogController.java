package com.edgeaiplatform.backend.controller;

import com.edgeaiplatform.backend.dto.ApiResponse;
import com.edgeaiplatform.backend.service.CatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService catalogService;
    public CatalogController(CatalogService catalogService) { this.catalogService = catalogService; }

    @GetMapping("/models") public ApiResponse<List<Object>> models() { return new ApiResponse<>(catalogService.models()); }
    @GetMapping("/hardware") public ApiResponse<List<Object>> hardware() { return new ApiResponse<>(catalogService.hardware()); }
    @GetMapping("/compatibility-reports") public ApiResponse<List<Object>> compatibilityReports() { return new ApiResponse<>(catalogService.compatibilityReports()); }
    @GetMapping("/deployments") public ApiResponse<List<Object>> deployments() { return new ApiResponse<>(catalogService.deployments()); }
}
