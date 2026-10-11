package com.edgeaiplatform.backend.service;

import com.edgeaiplatform.backend.model.ModelInspection;
import java.nio.file.Path;

/** Testable boundary for the separate Python ONNX worker; no process/HTTP adapter is provided yet. */
public interface ModelInspector {
    ModelInspection inspect(Path modelFile) throws ModelInspectionException;
}
