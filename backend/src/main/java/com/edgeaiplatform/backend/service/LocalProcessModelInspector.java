package com.edgeaiplatform.backend.service;

import com.edgeaiplatform.backend.config.InspectorProperties;
import com.edgeaiplatform.backend.model.ModelInspection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LocalProcessModelInspector implements ModelInspector {
    private final List<String> commandPrefix;
    private final long timeoutMillis;
    private final int maxOutputBytes;
    private final InspectorOutputParser parser;

    @Autowired
    public LocalProcessModelInspector(InspectorProperties properties) {
        this(properties, new InspectorOutputParser());
    }

    LocalProcessModelInspector(InspectorProperties properties, InspectorOutputParser parser) {
        this.commandPrefix = properties.command() == null ? List.of() : List.copyOf(properties.command());
        this.timeoutMillis = properties.timeout() == null ? 30_000 : properties.timeout().toMillis();
        this.maxOutputBytes = properties.maxOutputBytes() <= 0 ? 1_048_576 : properties.maxOutputBytes();
        this.parser = parser;
    }

    @Override
    public ModelInspection inspect(Path modelFile) throws ModelInspectionException {
        if (commandPrefix.isEmpty()) {
            throw new ModelInspectionException("INSPECTOR_UNAVAILABLE",
                "The model inspector is not configured.");
        }
        List<String> command = new ArrayList<>(commandPrefix);
        command.add(modelFile.toAbsolutePath().toString());
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process;
        try {
            process = builder.start();
        } catch (IOException ex) {
            throw new ModelInspectionException("INSPECTOR_UNAVAILABLE",
                "The model inspector could not be started. Install the worker and check app.inspector.command.");
        }
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        Thread stdoutReader = startDaemon(() -> drain(process.getInputStream(), stdout, maxOutputBytes + 1));
        Thread stderrReader = startDaemon(() -> drain(process.getErrorStream(), new ByteArrayOutputStream(), maxOutputBytes));
        try {
            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new ModelInspectionException("INSPECTOR_TIMEOUT",
                    "The model inspector did not finish within the configured timeout.");
            }
            stdoutReader.join(TimeUnit.SECONDS.toMillis(1));
            stderrReader.join(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new ModelInspectionException("INSPECTOR_TIMEOUT",
                "The model inspector did not finish within the configured timeout.");
        }
        byte[] bytes = stdout.toByteArray();
        if (bytes.length > maxOutputBytes) {
            throw new ModelInspectionException("INSPECTOR_INVALID_OUTPUT",
                "The model inspector returned unreadable output.");
        }
        String output = new String(bytes, StandardCharsets.UTF_8);
        if (output.isBlank()) {
            throw new ModelInspectionException("INSPECTOR_INVALID_OUTPUT",
                "The model inspector returned unreadable output.");
        }
        return parser.parse(output);
    }

    private static Thread startDaemon(Runnable task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void drain(java.io.InputStream stream, ByteArrayOutputStream target, int maxBytes) {
        try {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                int allowed = Math.max(0, maxBytes - target.size());
                target.write(buffer, 0, Math.min(read, allowed));
                if (read > allowed) {
                    return;
                }
            }
        } catch (IOException ignored) {
            // Process teardown can close the stream while draining.
        }
    }
}
