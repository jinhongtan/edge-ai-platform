package com.edgeaiplatform.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.junit.jupiter.api.Test;

class InspectorOutputParserTest {
    private final InspectorOutputParser parser = new InspectorOutputParser();

    @Test void parsesWorkerSuccessPayload() throws Exception {
        var json = """
            {"success":true,"data":{"valid":true,"file_size_bytes":88,"ir_version":9,"opsets":[{"domain":"","version":13}],"inputs":[{"name":"features","element_type":"FLOAT","shape":[1,3]}],"outputs":[{"name":"scores","element_type":"FLOAT","shape":["N",null]}],"operators":["MatMul"],"operator_counts":{"MatMul":1},"parameter_count":6}}
            """;
        var inspection = parser.parse(json);
        assertEquals(88, inspection.fileSizeBytes());
        assertEquals(9, inspection.irVersion());
        assertEquals("", inspection.opsets().getFirst().domain());
        assertEquals("features", inspection.inputs().getFirst().name());
        assertEquals(1L, inspection.inputs().getFirst().shape().getFirst());
        assertEquals("N", inspection.outputs().getFirst().shape().getFirst());
        assertNull(inspection.outputs().getFirst().shape().get(1));
        assertEquals(List.of("MatMul"), inspection.operators());
        assertEquals(1L, inspection.operatorCounts().get("MatMul"));
        assertEquals(6, inspection.parameterCount());
    }

    @Test void mapsWorkerErrorWithoutLeakingDetails() {
        var error = assertThrows(ModelInspectionException.class,
            () -> parser.parse("{\"success\":false,\"error\":{\"code\":\"INVALID_ONNX\",\"message\":\"File is not a valid ONNX model. Verify the file and export it again.\"}}"));
        assertEquals("INVALID_ONNX", error.code());
        assertEquals("File is not a valid ONNX model. Verify the file and export it again.", error.getMessage());
    }

    @Test void rejectsMalformedOutput() {
        var error = assertThrows(ModelInspectionException.class, () -> parser.parse("not-json"));
        assertEquals("INSPECTOR_INVALID_OUTPUT", error.code());
    }
}
