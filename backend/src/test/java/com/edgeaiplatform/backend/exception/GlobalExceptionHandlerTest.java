package com.edgeaiplatform.backend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {
    @Test void validationErrorsReturnStructuredBadRequest() {
        var response = new GlobalExceptionHandler().invalidParameter(new ConstraintViolationException(Set.of()));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().code());
    }

    @Test void inspectionErrorsKeepWorkerCodes() {
        var response = new GlobalExceptionHandler().inspection(
                new com.edgeaiplatform.backend.service.ModelInspectionException("FILE_TOO_LARGE", "too big"));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals("FILE_TOO_LARGE", response.getBody().code());
    }
}
