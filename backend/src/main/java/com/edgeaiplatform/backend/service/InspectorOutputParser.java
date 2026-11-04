package com.edgeaiplatform.backend.service;

import com.edgeaiplatform.backend.model.ModelInspection;
import com.edgeaiplatform.backend.model.OpsetMetadata;
import com.edgeaiplatform.backend.model.TensorMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InspectorOutputParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModelInspection parse(String json) throws ModelInspectionException {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (IOException ex) {
            throw new ModelInspectionException("INSPECTOR_INVALID_OUTPUT", "The model inspector returned unreadable output.");
        }
        if (root == null || root.isNull() || root.isMissingNode()) {
            throw new ModelInspectionException("INSPECTOR_INVALID_OUTPUT", "The model inspector returned unreadable output.");
        }
        if (!root.path("success").asBoolean(false)) {
            JsonNode error = root.path("error");
            String code = textOr(error.path("code"), "INVALID_ONNX");
            String message = textOr(error.path("message"), "Model inspection failed.");
            throw new ModelInspectionException(code, message);
        }
        JsonNode data = root.path("data");
        if (!data.isObject()) {
            throw new ModelInspectionException("INSPECTOR_INVALID_OUTPUT", "The model inspector returned unreadable output.");
        }
        try {
            return new ModelInspection(
                data.path("file_size_bytes").asLong(),
                data.path("ir_version").asLong(),
                opsets(data.path("opsets")),
                tensors(data.path("inputs")),
                tensors(data.path("outputs")),
                strings(data.path("operators")),
                operatorCounts(data.path("operator_counts")),
                data.path("parameter_count").asLong());
        } catch (IllegalArgumentException ex) {
            throw new ModelInspectionException("INSPECTOR_INVALID_OUTPUT", "The model inspector returned unreadable output.");
        }
    }

    private static List<OpsetMetadata> opsets(JsonNode node) {
        List<OpsetMetadata> values = new ArrayList<>();
        if (!node.isArray()) {
            return List.of();
        }
        for (JsonNode item : node) {
            values.add(new OpsetMetadata(item.path("domain").asText(""), item.path("version").asLong()));
        }
        return List.copyOf(values);
    }

    private static List<TensorMetadata> tensors(JsonNode node) {
        List<TensorMetadata> values = new ArrayList<>();
        if (!node.isArray()) {
            return List.of();
        }
        for (JsonNode item : node) {
            values.add(new TensorMetadata(
                item.path("name").asText(""),
                item.path("element_type").asText(""),
                shape(item.path("shape"))));
        }
        return List.copyOf(values);
    }

    private static List<Object> shape(JsonNode node) {
        List<Object> values = new ArrayList<>();
        if (!node.isArray()) {
            return List.of();
        }
        for (JsonNode item : node) {
            if (item.isNull()) {
                values.add(null);
            } else if (item.isNumber()) {
                values.add(item.asLong());
            } else {
                values.add(item.asText());
            }
        }
        return java.util.Collections.unmodifiableList(values);
    }

    private static List<String> strings(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (!node.isArray()) {
            return List.of();
        }
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText());
            }
        }
        return List.copyOf(values);
    }

    private static Map<String, Long> operatorCounts(JsonNode node) {
        Map<String, Long> values = new LinkedHashMap<>();
        if (!node.isObject()) {
            return Map.of();
        }
        node.fields().forEachRemaining(entry -> {
            if (entry.getValue().canConvertToLong() && entry.getValue().asLong() >= 0) {
                values.put(entry.getKey(), entry.getValue().asLong());
            }
        });
        return Map.copyOf(values);
    }

    private static String textOr(JsonNode node, String fallback) {
        String value = node.asText("");
        return value.isBlank() ? fallback : value;
    }
}
