package com.itmo.blps.lab1.controllers;

import org.springframework.beans.factory.annotation.Autowired;
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

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Authentication API")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Login to the system")
    public JwtDto login(@RequestBody @Valid AuthRequest request) {
        return new JwtDto(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Register a new user")
    public JwtDto register(@RequestBody @Valid AuthRequest request) {
        return new JwtDto(authService.register(request));
    }
}
