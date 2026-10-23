package com.edgeaiplatform.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BuildRequest(
    @NotBlank String modelId,
    @NotBlank String hardwareId,
    @NotBlank String precision,
    @NotNull Options options
) {
    public record Options(@Min(1) @Max(4096) int workspaceMiB) { }
}
