package com.edgeaiplatform.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.edgeaiplatform.backend.config.WorkerProperties;
import org.junit.jupiter.api.Test;

class LocalProcessModelInspectorTest {
    private final LocalProcessModelInspector inspector =
            new LocalProcessModelInspector(new WorkerProperties("python", "onnx_worker", 30, 1024, null));

    @Test void parsesSuccessfulWorkerJson() throws Exception {
        var inspection = inspector.parseWorkerOutput("""
                {"success":true,"data":{"valid":true,"file_size_bytes":128,"ir_version":9,
                "opsets":[{"domain":"","version":13}],
                "inputs":[{"name":"features","element_type":"FLOAT","shape":[1,3]}],
                "outputs":[{"name":"scores","element_type":"FLOAT","shape":[1,2]}]}}
                """);
        assertEquals(128, inspection.fileSizeBytes());
        assertEquals(9, inspection.irVersion());
        assertEquals("features", inspection.inputs().getFirst().name());
        assertEquals("FLOAT", inspection.inputs().getFirst().elementType());
        assertEquals(13, inspection.opsets().getFirst().version());
    }

    @Test void mapsWorkerErrorsWithoutPaths() {
        var error = assertThrows(ModelInspectionException.class, () -> inspector.parseWorkerOutput("""
                {"success":false,"error":{"code":"INVALID_ONNX","message":"File is not a valid ONNX model. Verify the file and export it again."}}
                """));
        assertEquals("INVALID_ONNX", error.code());
        assertEquals("File is not a valid ONNX model. Verify the file and export it again.", error.getMessage());
    }
}
