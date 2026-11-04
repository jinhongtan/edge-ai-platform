package com.edgeaiplatform.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import com.edgeaiplatform.backend.config.AuthProperties;
import com.edgeaiplatform.backend.dto.LoginRequest;
import com.edgeaiplatform.backend.dto.RegisterRequest;
import com.edgeaiplatform.backend.exception.AuthException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

class AuthServiceTest {
    @TempDir Path directory;

    @Test void registersLogsInAndResolvesCurrentUser() throws Exception {
        AuthService service = service();
        var registration = service.register(new RegisterRequest("User@Example.com", "Acme", "long-enough-password"));
        assertEquals("user@example.com", registration.user().email());
        assertEquals("user@example.com", service.currentUser(registration.accessToken()).email());
        assertNotEquals(registration.accessToken(), service.login(new LoginRequest("user@example.com", "long-enough-password")).accessToken());
    }

    @Test void rejectsDuplicateAndIncorrectCredentials() throws Exception {
        AuthService service = service();
        service.register(new RegisterRequest("user@example.com", "Acme", "long-enough-password"));
        assertEquals("EMAIL_ALREADY_REGISTERED", assertThrows(AuthException.class, () -> service.register(new RegisterRequest("user@example.com", "Acme", "long-enough-password"))).code());
        assertEquals("INVALID_CREDENTIALS", assertThrows(AuthException.class, () -> service.login(new LoginRequest("user@example.com", "wrong-password"))).code());
    }

    private AuthService service() throws Exception { AuthService service = new AuthService(new AuthProperties(directory, Duration.ofHours(1)), new ObjectMapper().findAndRegisterModules()); service.load(); return service; }
}
