package com.itmo.blps.labs.services.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.itmo.blps.labs.dto.auth.AuthRequest;
import com.itmo.blps.labs.entities.Role;
import com.itmo.blps.labs.entities.User;
import com.itmo.blps.labs.exception.BadRequestException;
import com.itmo.blps.labs.services.core.UserService;

@Service
public class AuthService {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    public String register(AuthRequest request) {
        if (userService.getUserByUsername(request.getUsername()) != null) {
            throw new BadRequestException("User already exists");
        }

        User user = userService.createUser(request, Role.USER);

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
