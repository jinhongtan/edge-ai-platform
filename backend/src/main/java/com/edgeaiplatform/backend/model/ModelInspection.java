package com.edgeaiplatform.backend.model;

import java.util.List;
import java.util.Map;

public record ModelInspection(long fileSizeBytes, long irVersion, List<OpsetMetadata> opsets,
                              List<TensorMetadata> inputs, List<TensorMetadata> outputs,
                              List<String> operators, Map<String, Long> operatorCounts,
                              long parameterCount) { }
