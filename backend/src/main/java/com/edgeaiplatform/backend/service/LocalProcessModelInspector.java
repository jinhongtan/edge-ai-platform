package com.edgeaiplatform.backend.service;

import com.edgeaiplatform.backend.config.WorkerProperties;
import com.edgeaiplatform.backend.model.ModelInspection;
import com.edgeaiplatform.backend.model.OpsetMetadata;
import com.edgeaiplatform.backend.model.TensorMetadata;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class LocalProcessModelInspector implements ModelInspector {
    private final WorkerProperties properties;
    private final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    public LocalProcessModelInspector(WorkerProperties properties) {
        this.properties = properties;
    }

    @Override
    public ModelInspection inspect(Path modelFile) throws ModelInspectionException {
        ProcessBuilder builder = new ProcessBuilder(
                properties.python(), "-m", properties.module(), modelFile.toAbsolutePath().toString());
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        Path workerSrc = resolveWorkerSrc();
        if (workerSrc != null) {
            builder.environment().merge("PYTHONPATH", workerSrc.toString(), (existing, added) ->
                    added + File.pathSeparator + existing);
        }
        Process process;
        try {
            process = builder.start();
        } catch (IOException ex) {
            throw new ModelInspectionException("DEPENDENCY_MISSING",
                    "Python ONNX worker could not be started. Install the worker with: python -m pip install -e '.[test]'.");
        }
        try {
            String stdout = readCapped(process.getInputStream(), properties.maxOutputBytes());
            boolean finished = process.waitFor(properties.timeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ModelInspectionException("WORKER_TIMEOUT", "Model inspection timed out. Try a smaller file or retry.");
            }
            return parseWorkerOutput(stdout);
        } catch (InterruptedException ex) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new ModelInspectionException("WORKER_TIMEOUT", "Model inspection was interrupted.");
        } catch (IOException ex) {
            process.destroyForcibly();
            throw new ModelInspectionException("WORKER_UNAVAILABLE", "Model inspection failed unexpectedly.");
        }
    }

    ModelInspection parseWorkerOutput(String stdout) throws ModelInspectionException {
        if (stdout == null || stdout.isBlank()) {
            throw new ModelInspectionException("WORKER_UNAVAILABLE", "Model inspection returned no result.");
        }
        WorkerResponse response;
        try {
            response = mapper.readValue(stdout, WorkerResponse.class);
        } catch (IOException ex) {
            throw new ModelInspectionException("WORKER_UNAVAILABLE", "Model inspection returned an unreadable result.");
        }
        if (response.error() != null && response.error().code() != null) {
            String message = response.error().message() == null ? "Model inspection failed." : response.error().message();
            throw new ModelInspectionException(response.error().code(), message);
        }
        if (!response.success() || response.data() == null) {
            throw new ModelInspectionException("WORKER_UNAVAILABLE", "Model inspection returned no result.");
        }
        WorkerData data = response.data();
        return new ModelInspection(
                data.fileSizeBytes(),
                data.irVersion(),
                data.opsets() == null ? List.of() : data.opsets().stream()
                        .map(item -> new OpsetMetadata(item.domain() == null ? "" : item.domain(), item.version()))
                        .toList(),
                mapTensors(data.inputs()),
                mapTensors(data.outputs()));
    }

    private static List<TensorMetadata> mapTensors(List<WorkerTensor> tensors) {
        if (tensors == null) {
            return List.of();
        }
        return tensors.stream()
                .map(tensor -> new TensorMetadata(tensor.name(), tensor.elementType(),
                        tensor.shape() == null ? List.of() : List.copyOf(tensor.shape())))
                .toList();
    }

    private Path resolveWorkerSrc() {
        if (properties.workingDirectory() != null) {
            Path configured = Path.of(properties.workingDirectory()).resolve("src").toAbsolutePath().normalize();
            return Files.isDirectory(configured) ? configured : null;
        }
        Path userDir = Path.of("").toAbsolutePath();
        for (Path candidate : List.of(userDir.resolve("model-worker"), userDir.resolve("../model-worker"))) {
            Path src = candidate.resolve("src").toAbsolutePath().normalize();
            if (Files.isDirectory(src.resolve("onnx_worker"))) {
                return src;
            }
        }
        return null;
    }

    private static String readCapped(InputStream in, int max) throws IOException, ModelInspectionException {
        byte[] buf = in.readNBytes(max + 1);
        if (buf.length > max) {
            throw new ModelInspectionException("WORKER_UNAVAILABLE", "Model inspection produced too much output.");
        }
        return new String(buf, StandardCharsets.UTF_8);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WorkerResponse(boolean success, WorkerData data, WorkerError error) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WorkerError(String code, String message) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WorkerData(long fileSizeBytes, long irVersion, List<WorkerOpset> opsets,
                              List<WorkerTensor> inputs, List<WorkerTensor> outputs) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WorkerOpset(String domain, long version) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WorkerTensor(String name, String elementType, List<Object> shape) { }
}
