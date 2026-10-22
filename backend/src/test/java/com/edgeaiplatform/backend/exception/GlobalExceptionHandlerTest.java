package com.edgeaiplatform.backend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.edgeaiplatform.backend.service.ModelInspectionException;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test void validationErrorsReturnStructuredBadRequest() {
        var response = handler.invalidParameter(new ConstraintViolationException(Set.of()));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().code());
    }

    @Test void uploadRejectionUsesProvidedCode() {
        var response = handler.rejectedUpload(new UploadRejectedException("EMPTY_FILE", "Choose an ONNX model file to upload."));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("EMPTY_FILE", response.getBody().code());
    }

    @Test void missingInspectorIsServiceUnavailable() {
        var response = handler.inspectionFailed(new ModelInspectionException("INSPECTOR_UNAVAILABLE", "The model inspector could not be started."));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("INSPECTOR_UNAVAILABLE", response.getBody().code());
    }
}
