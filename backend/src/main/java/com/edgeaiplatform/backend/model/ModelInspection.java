package com.edgeaiplatform.backend.model;

import java.util.List;

public record ModelInspection(long fileSizeBytes, long irVersion, List<OpsetMetadata> opsets,
                              List<TensorMetadata> inputs, List<TensorMetadata> outputs) { }
