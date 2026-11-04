package com.edgeaiplatform.backend.controller;

import com.edgeaiplatform.backend.dto.*;
import com.edgeaiplatform.backend.exception.AuthException;
import com.edgeaiplatform.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<AuthSession> register(@Valid @RequestBody RegisterRequest request) { return new ApiResponse<>(service.register(request)); }
    @PostMapping("/login") public ApiResponse<AuthSession> login(@Valid @RequestBody LoginRequest request) { return new ApiResponse<>(service.login(request)); }
    @GetMapping("/me") public ApiResponse<UserIdentity> me(@RequestHeader(value = "Authorization", required = false) String authorization) { return new ApiResponse<>(service.currentUser(bearer(authorization))); }
    private static String bearer(String value) { if (value == null || !value.startsWith("Bearer ") || value.length() <= 7) throw new AuthException(401, "INVALID_SESSION", "Bearer access token is required."); return value.substring(7); }
}
