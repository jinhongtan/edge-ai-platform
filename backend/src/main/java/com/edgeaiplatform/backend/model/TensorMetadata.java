package com.edgeaiplatform.backend.model;

import java.util.List;

public record TensorMetadata(String name, String elementType, List<Object> shape) { }
