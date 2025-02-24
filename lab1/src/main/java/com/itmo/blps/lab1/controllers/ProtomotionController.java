package com.itmo.blps.lab1.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.services.core.PromotionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/promotions")
@Tag(name = "Promotion", description = "Promotion Management API")
public class ProtomotionController {

    @Autowired
    private PromotionService promotionService;

    @PostMapping
    @Operation(summary = "Create a new promotion", description = "Creates a new promotion with the provided details")
    @ApiResponse(responseCode = "200", description = "Promotion created successfully")
    public Promotion createPromotion(@RequestBody Promotion promotion) {
        return promotionService.createPromotion(promotion);
    }

    @GetMapping
    @Operation(summary = "Get all promotions", description = "Retrieves all promotions")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all promotions")
    public List<Promotion> getAllPromotions() {
        return promotionService.getAllPromotions();
    }

    @GetMapping("/active")
    @Operation(summary = "Get active promotions", description = "Retrieves all active promotions")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved active promotions")
    public List<Promotion> getActivePromotions() {
        return promotionService.getActivePromotions();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get promotion by ID", description = "Retrieves a promotion by its ID")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the promotion")
    public ResponseEntity<Promotion> getPromotionById(@PathVariable Long id) {
        return promotionService.getPromotionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update promotion", description = "Updates an existing promotion")
    @ApiResponse(responseCode = "200", description = "Promotion updated successfully")
    public Promotion updatePromotion(@PathVariable Long id, @RequestBody Promotion promotion) {
        return promotionService.updatePromotion(id, promotion);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete promotion", description = "Deletes an existing promotion")
    @ApiResponse(responseCode = "200", description = "Promotion deleted successfully")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok().build();
    }
}
