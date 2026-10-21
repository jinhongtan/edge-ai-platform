package com.edgeaiplatform.backend.controller;

import com.edgeaiplatform.backend.dto.ApiResponse;
import com.edgeaiplatform.backend.dto.ModelSummary;
import com.edgeaiplatform.backend.service.CatalogService;
import com.edgeaiplatform.backend.service.ModelInspectionException;
import com.edgeaiplatform.backend.service.ModelService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService catalogService;
    private final ModelService modelService;

    public CatalogController(CatalogService catalogService, ModelService modelService) {
        this.catalogService = catalogService;
        this.modelService = modelService;
    }

    @GetMapping("/models")
    public ApiResponse<List<ModelSummary>> models() {
        return new ApiResponse<>(modelService.models());
    }

    @PostMapping(path = "/models", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ModelSummary>> upload(@RequestParam("file") MultipartFile file)
            throws ModelInspectionException {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(modelService.upload(file)));
    }

    @GetMapping("/hardware") public ApiResponse<List<Object>> hardware() { return new ApiResponse<>(catalogService.hardware()); }
    @GetMapping("/compatibility-reports") public ApiResponse<List<Object>> compatibilityReports() { return new ApiResponse<>(catalogService.compatibilityReports()); }
    @GetMapping("/deployments") public ApiResponse<List<Object>> deployments() { return new ApiResponse<>(catalogService.deployments()); }
}
