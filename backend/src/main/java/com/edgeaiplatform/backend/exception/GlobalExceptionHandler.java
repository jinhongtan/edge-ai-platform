package com.edgeaiplatform.backend.exception;

import com.edgeaiplatform.backend.dto.ApiError;
import com.edgeaiplatform.backend.service.ModelInspectionException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream().collect(java.util.stream.Collectors.toMap(
            error -> error.getField(), error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(), (a, b) -> a));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Request validation failed", details, Instant.now()));
    }
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> invalidParameter(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Request validation failed", Map.of(), Instant.now()));
    }
    @ExceptionHandler(ModelInspectionException.class)
    public ResponseEntity<ApiError> inspection(ModelInspectionException ex) {
        HttpStatus status = switch (ex.code()) {
            case "FILE_TOO_LARGE" -> HttpStatus.PAYLOAD_TOO_LARGE;
            case "DEPENDENCY_MISSING", "WORKER_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "WORKER_TIMEOUT" -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(new ApiError(ex.code(), ex.getMessage(), Map.of(), Instant.now()));
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> missingFile() {
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "An ONNX file is required.", Map.of("file", "Required"), Instant.now()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> tooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiError("FILE_TOO_LARGE", "Model exceeds the configured upload size limit.", Map.of(), Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError("INTERNAL_ERROR", "An unexpected error occurred", Map.of(), Instant.now()));
    }
}
