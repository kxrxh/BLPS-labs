package com.itmo.blps.lab1.security;

import com.itmo.blps.lab1.entities.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class UserAuthentication extends UsernamePasswordAuthenticationToken {

    private final Long userId;

    public UserAuthentication(User principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        this.userId = principal.getId();
    }

    public Long getUserId() {
        return userId;
    }

    // Optionally override getPrincipal to return User if needed elsewhere,
    // but ensure type safety or casting when using it.
    @Override
    public User getPrincipal() {
        return (User) super.getPrincipal();
    }
} 