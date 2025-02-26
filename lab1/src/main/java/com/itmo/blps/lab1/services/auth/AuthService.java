package com.itmo.blps.lab1.services.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.itmo.blps.lab1.dto.auth.AuthRequest;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.exception.BadRequestException;
import com.itmo.blps.lab1.services.core.UserService;

@Service
public class AuthService {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String register(AuthRequest request) {
        if (userService.getUserByUsername(request.getUsername()) != null) {
            throw new BadRequestException("User already exists");
        }
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userService.save(user);

        var jwtToken = jwtService.generateToken(user);
        return jwtToken;
    }

    public String login(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        var user = userService
                .userDetailsService()
                .loadUserByUsername(request.getUsername());

            var jwtToken = jwtService.generateToken(user);
            return jwtToken;
        } catch (BadCredentialsException e) {
            return "Invalid credentials";
        }
    }
}
