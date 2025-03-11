package com.itmo.blps.labs.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itmo.blps.labs.entities.Promotion;
import com.itmo.blps.labs.dto.PromotionDto;
import com.itmo.blps.labs.dto.error.ErrorResponse;
import com.itmo.blps.labs.services.core.PromotionService;

import io.basc.framework.lang.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/promotions")
@Tag(name = "Promotion", description = "Promotion Management API")
@SecurityRequirement(name = "Bearer Authentication")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Create a new promotion", description = "Creates a new promotion with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    public Promotion createPromotion(@RequestBody @Valid PromotionDto promotionDto) {
        return promotionService.createPromotion(promotionDto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MODERATOR')")
    @Operation(summary = "Get all promotions", description = "Retrieves all promotions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all promotions"),
    })
    public List<Promotion> getAllPromotions() {
        return promotionService.getAllPromotions();
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MODERATOR')")
    @Operation(summary = "Get active promotions", description = "Retrieves all active promotions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved active promotions"),
    })
    public List<Promotion> getActivePromotions() {
        return promotionService.getActivePromotions();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MODERATOR')")
    @Operation(summary = "Get promotion by ID", description = "Retrieves a promotion by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the promotion"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Promotion getPromotionById(@PathVariable Long id) {
        return promotionService.getPromotionById(id)
                .orElseThrow(() -> new NotFoundException("Promotion not found with id: " + id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MODERATOR')")
    @Operation(summary = "Update promotion", description = "Updates an existing promotion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or ID format", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Promotion updatePromotion(@PathVariable Long id, @RequestBody @Valid PromotionDto promotionDto) {
        return promotionService.updatePromotion(id, promotionDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Delete promotion", description = "Deletes an existing promotion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion deleted successfully"),
    })
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok().build();
    }
}
