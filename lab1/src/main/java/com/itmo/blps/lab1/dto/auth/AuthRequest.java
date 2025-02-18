package com.itmo.blps.lab1.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Authentication request")
public class AuthRequest {
    @Schema(description = "Username", example = "user")
    @NotBlank(message = "Username is required")
    @Size(min = 3, message = "Username must be at least 3 characters long")
    private String username;

    @Schema(description = "Password", example = "password")
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
}
