package com.edgeaiplatform.backend.service;

import com.edgeaiplatform.backend.dto.*;
import com.edgeaiplatform.backend.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CompatibilityService {
    private final FileSystemModelStore store;
    private final HardwareProfileService profiles;
    public CompatibilityService(FileSystemModelStore store, HardwareProfileService profiles) { this.store = store; this.profiles = profiles; }

    public CompatibilityResult check(String modelId, String hardwareId) {
        var model = store.find(modelId).orElseThrow(() -> new ResourceNotFoundException("Model was not found."));
        var profile = profiles.find(hardwareId);
        if (model.inspection() == null) throw new IllegalStateException("Model analysis is not available.");
        var inspection = model.inspection();
        var unsupported = new ArrayList<UnsupportedOperator>();
        (inspection.operatorCounts() == null ? Map.<String, Long>of() : inspection.operatorCounts()).forEach((operator, count) -> { if (!profile.supportedOperators().contains(operator)) unsupported.add(new UnsupportedOperator(operator, count, "Unsupported operator")); });
        var warnings = new ArrayList<String>();
        inspection.inputs().forEach(input -> { if (input.shape().stream().anyMatch(dimension -> dimension == null || dimension instanceof String)) warnings.add("Dynamic input shape detected for " + input.name() + ". Additional runtime/build configuration may be required."); });
        var precisions = new ArrayList<String>();
        inspection.inputs().forEach(tensor -> precision(tensor.elementType(), profile, unsupported, precisions));
        inspection.outputs().forEach(tensor -> precision(tensor.elementType(), profile, unsupported, precisions));
        inspection.opsets().forEach(opset -> { if (opset.domain().isEmpty() && (opset.version() < profile.minimumOpset() || opset.version() > profile.maximumOpset())) unsupported.add(new UnsupportedOperator("ONNX opset " + opset.version(), 1, "Outside configured supported opset range")); });
        String status = !unsupported.isEmpty() ? "INCOMPATIBLE" : !warnings.isEmpty() ? "WARNING" : "COMPATIBLE";
        var result = new CompatibilityResult(UUID.randomUUID().toString(), modelId, hardwareId, status, List.copyOf(unsupported), List.copyOf(warnings), profile.supportedPrecisions(), Instant.now());
        try { return store.saveCompatibility(result); } catch (java.io.IOException ex) { throw new IllegalStateException("Compatibility result could not be stored."); }
    }
    public CompatibilityResult latest(String modelId) { if (store.find(modelId).isEmpty()) throw new ResourceNotFoundException("Model was not found."); return store.latestCompatibility(modelId).orElseThrow(() -> new ResourceNotFoundException("Compatibility result was not found.")); }
    private static void precision(String type, HardwareProfile profile, List<UnsupportedOperator> unsupported, List<String> found) {
        String precision = switch (type) { case "FLOAT" -> "FP32"; case "FLOAT16" -> "FP16"; case "INT8", "UINT8" -> "INT8"; default -> null; };
        if (precision == null || !profile.supportedPrecisions().contains(precision)) unsupported.add(new UnsupportedOperator(type, 1, "Unsupported input or output precision"));
        else if (!found.contains(precision)) found.add(precision);
    }
}
