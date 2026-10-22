package com.edgeaiplatform.backend.service;

import com.edgeaiplatform.backend.dto.HardwareProfile;
import com.edgeaiplatform.backend.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class HardwareProfileService {
    private static final HardwareProfile JETSON = new HardwareProfile("jetson-orin-nano-super", "NVIDIA",
        "Jetson Orin Nano Super Developer Kit", "NVIDIA Ampere", "TensorRT",
        List.of("FP32", "FP16", "INT8"),
        Set.of("Add", "Cast", "Clip", "Concat", "Conv", "ConvInteger", "Div", "DynamicQuantizeLinear", "Flatten", "Gemm", "GlobalAveragePool", "MatMul", "MatMulInteger", "MaxPool", "Mul", "ReduceMean", "Relu", "Reshape", "Sigmoid", "Softmax", "Squeeze", "Sub", "Transpose", "Unsqueeze"),
        13, 21, "Initial configurable TensorRT-oriented profile. Compatibility is static analysis only; it is not an engine build or hardware validation.");

    public List<HardwareProfile> list() { return List.of(JETSON); }
    public HardwareProfile find(String id) {
        if (JETSON.id().equals(id)) return JETSON;
        throw new ResourceNotFoundException("Hardware profile was not found.");
    }
}
