package com.edgeaiplatform.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.edgeaiplatform.backend.config.InspectorProperties;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class LocalProcessModelInspectorTest {
    @TempDir Path tempDir;

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void timesOutHungWindowsProcess() {
        var inspector = new LocalProcessModelInspector(new InspectorProperties(
            List.of("cmd.exe", "/c", "ping -n 20 127.0.0.1 & rem"), Duration.ofMillis(200), 1024));
        var error = assertThrows(ModelInspectionException.class, () -> inspector.inspect(tempDir.resolve("model.onnx")));
        assertEquals("INSPECTOR_TIMEOUT", error.code());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void timesOutHungUnixProcess() {
        var inspector = new LocalProcessModelInspector(new InspectorProperties(
            List.of("sleep", "20"), Duration.ofMillis(200), 1024));
        var error = assertThrows(ModelInspectionException.class, () -> inspector.inspect(tempDir.resolve("model.onnx")));
        assertEquals("INSPECTOR_TIMEOUT", error.code());
    }

    @Test void missingExecutableIsUnavailable() {
        var inspector = new LocalProcessModelInspector(new InspectorProperties(
            List.of("edge-ai-missing-inspector-binary"), Duration.ofSeconds(2), 1024));
        var error = assertThrows(ModelInspectionException.class, () -> inspector.inspect(tempDir.resolve("model.onnx")));
        assertEquals("INSPECTOR_UNAVAILABLE", error.code());
        assertFalseContainsPath(error.getMessage(), tempDir);
    }

    private static void assertFalseContainsPath(String message, Path tempDir) {
        org.junit.jupiter.api.Assertions.assertFalse(message.contains(tempDir.toString()));
    }
}
