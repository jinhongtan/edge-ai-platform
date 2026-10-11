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
}
