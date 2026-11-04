package com.edgeaiplatform.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank @Email String email, @NotBlank @Size(max = 120) String organization,
                              @NotBlank @Size(min = 12, max = 256) String password) { }
