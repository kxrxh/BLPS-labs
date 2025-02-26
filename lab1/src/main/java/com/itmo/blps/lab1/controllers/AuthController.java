package com.itmo.blps.lab1.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itmo.blps.lab1.dto.auth.AuthRequest;
import com.itmo.blps.lab1.dto.auth.JwtDto;
import com.itmo.blps.lab1.services.auth.AuthService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Authentication API")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Login to the system")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "403", description = "Invalid credentials")
    public ResponseEntity<?> login(@RequestBody @Valid AuthRequest request) {
        return ResponseEntity.ok(new JwtDto(authService.login(request)));
    }

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Register a new user")
    @ApiResponse(responseCode = "200", description = "Register successful")
    @ApiResponse(responseCode = "403", description = "Invalid request")
    public ResponseEntity<?> register(@RequestBody @Valid AuthRequest request) {
        if (authService.register(request) == "User already exists") {
            throw new BadCredentialsException("User already exists");
        }
        return ResponseEntity.ok(new JwtDto(authService.register(request)));
    }
}
