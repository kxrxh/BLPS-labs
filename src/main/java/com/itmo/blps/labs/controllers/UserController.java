package com.itmo.blps.labs.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itmo.blps.labs.dto.auth.AuthRequest;
import com.itmo.blps.labs.entities.Advertisement;
import com.itmo.blps.labs.entities.Role;
import com.itmo.blps.labs.entities.User;
import com.itmo.blps.labs.services.core.AdvertisementService;
import com.itmo.blps.labs.services.core.UserService;

import io.basc.framework.context.ioc.annotation.Autowired;
import io.basc.framework.web.message.annotation.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@Tag(name = "User", description = "User Management API")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AdvertisementService advertisementService;

    @GetMapping
    @RolesAllowed("ROLE_ADMIN")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/{id}/advertisements")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_MODERATOR" })
    public ResponseEntity<List<Advertisement>> getUserAdvertisements(@PathVariable Long id) {
        return ResponseEntity.ok(advertisementService.getAdvertisementsByAuthor(id));
    }

    @PutMapping("/{id}")
    @RolesAllowed("ROLE_ADMIN")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @PostMapping("/admin")
    @RolesAllowed("ROLE_ADMIN")
    public ResponseEntity<User> createUser(@RequestBody @Valid AuthRequest request) {
        return ResponseEntity.ok(userService.createUser(request, Role.ADMIN));
    }

    @PostMapping("/moderator")
    @RolesAllowed("ROLE_ADMIN")
    public ResponseEntity<User> createModerator(@RequestBody @Valid AuthRequest request) {
        return ResponseEntity.ok(userService.createUser(request, Role.MODERATOR));
    }

    @PatchMapping("/{id}/password")
    @PermitAll
    public ResponseEntity<User> updateUserPassword(@PathVariable Long id, @RequestBody String password) {
        return ResponseEntity.ok(userService.updateUserPassword(id, password));
    }

    @DeleteMapping("/{id}")
    @RolesAllowed("ROLE_ADMIN")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

}
