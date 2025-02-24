package com.itmo.blps.lab1.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.dto.PromotionDto;
import com.itmo.blps.lab1.dto.error.ErrorResponse;
import com.itmo.blps.lab1.services.core.PromotionService;

import io.basc.framework.lang.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/promotions")
@Tag(name = "Promotion", description = "Promotion Management API")
@SecurityRequirement(name = "Bearer Authentication")
public class ProtomotionController {

    @Autowired
    private PromotionService promotionService;

    @PostMapping
    @Operation(summary = "Create a new promotion", description = "Creates a new promotion with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    public Promotion createPromotion(@RequestBody PromotionDto promotionDto) {
        return promotionService.createPromotion(promotionDto);
    }

    @GetMapping
    @Operation(summary = "Get all promotions", description = "Retrieves all promotions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all promotions"),
    })
    public List<Promotion> getAllPromotions() {
        return promotionService.getAllPromotions();
    }

    @GetMapping("/active")
    @Operation(summary = "Get active promotions", description = "Retrieves all active promotions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved active promotions"),
    })
    public List<Promotion> getActivePromotions() {
        return promotionService.getActivePromotions();
    }

    @GetMapping("/{id}")
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
    @Operation(summary = "Update promotion", description = "Updates an existing promotion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or ID format", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Promotion updatePromotion(@PathVariable Long id, @RequestBody PromotionDto promotionDto) {
        return promotionService.updatePromotion(id, promotionDto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete promotion", description = "Deletes an existing promotion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate promotion", description = "Activates an existing promotion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion activated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void activatePromotion(@PathVariable Long id) {
        promotionService.activatePromotion(id);
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate promotion", description = "Deactivates an existing promotion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion deactivated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void deactivatePromotion(@PathVariable Long id) {
        promotionService.deactivatePromotion(id);
    }
}
