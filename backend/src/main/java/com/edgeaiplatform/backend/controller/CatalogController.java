package com.edgeaiplatform.backend.controller;

import com.edgeaiplatform.backend.dto.ApiResponse;
import com.edgeaiplatform.backend.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService catalogService;
    public CatalogController(CatalogService catalogService) { this.catalogService = catalogService; }

    @GetMapping("/compatibility-reports") public ApiResponse<java.util.List<Object>> compatibilityReports() { return new ApiResponse<>(catalogService.compatibilityReports()); }
    @GetMapping("/deployments") public ApiResponse<java.util.List<Object>> deployments() { return new ApiResponse<>(catalogService.deployments()); }
}
