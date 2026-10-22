package com.edgeaiplatform.backend.dto;

import java.util.List;
import java.util.Set;

public record HardwareProfile(String id, String vendor, String name, String architecture, String runtime,
                              List<String> supportedPrecisions, Set<String> supportedOperators,
                              Integer minimumOpset, Integer maximumOpset, String description) { }
