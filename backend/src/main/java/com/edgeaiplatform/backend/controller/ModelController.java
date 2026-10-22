package com.edgeaiplatform.backend.controller;

import com.edgeaiplatform.backend.dto.ApiResponse;
import com.edgeaiplatform.backend.dto.ModelSummary;
import com.edgeaiplatform.backend.service.ModelService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/models")
public class ModelController {
    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping
    public ApiResponse<List<ModelSummary>> list() {
        return new ApiResponse<>(modelService.list());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ModelSummary> upload(@RequestParam("file") MultipartFile file) {
        return new ApiResponse<>(modelService.upload(file));
    }
}
