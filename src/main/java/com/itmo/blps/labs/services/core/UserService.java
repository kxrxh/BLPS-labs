package com.itmo.blps.labs.services.core;

import java.util.Collections;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itmo.blps.labs.dto.auth.AuthRequest;
import com.itmo.blps.labs.entities.Role;
import com.itmo.blps.labs.entities.User;
import com.itmo.blps.labs.repositories.UserRepository;
import io.basc.framework.lang.NotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public UserDetailsService userDetailsService() {
        return username -> getUserByUsername(username);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User updatedUser) {
        User existingUser = getUserById(id);
        
        // Update user fields
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setRoles(updatedUser.getRoles());
        
        return userRepository.save(existingUser);
    }

    @Transactional
    public User updateUserPassword(Long id, String newPassword) {
        User user = getUserById(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public User createUser(AuthRequest request, Role role) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Collections.singleton(role));
        return userRepository.save(user);
    }
}
