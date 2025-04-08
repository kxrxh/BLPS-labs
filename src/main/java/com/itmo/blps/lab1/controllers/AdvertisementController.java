package com.itmo.blps.lab1.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.itmo.blps.lab1.dto.AdDto;
import com.itmo.blps.lab1.dto.AdvertisementResponseDto;
import com.itmo.blps.lab1.dto.error.ErrorResponse;
import com.itmo.blps.lab1.services.core.AdvertisementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/advertisements")
@Tag(name = "Advertisement", description = "Advertisement API")
public class AdvertisementController {

    @Autowired
    private AdvertisementService advertisementService;

    @PostMapping
    @Operation(summary = "Create a new advertisement", description = "Creates a new advertisement with the provided details")
    @ApiResponse(responseCode = "200", description = "Advertisement created successfully")
    public AdvertisementResponseDto createAdvertisement(@RequestBody @Valid AdDto adDto) {
        return advertisementService.createAdvertisement(adDto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get advertisement by ID", description = "Retrieves an advertisement by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the advertisement"),
            @ApiResponse(responseCode = "404", description = "Advertisement not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdvertisementResponseDto> getAdvertisement(@PathVariable Long id) {
        return advertisementService.getAdvertisementById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all advertisements", description = "Retrieves all advertisements")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all advertisements"),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<AdvertisementResponseDto> getAllAdvertisements() {
        return advertisementService.getAllAdvertisements();
    }

    @PatchMapping("/promotion")
    @Operation(summary = "Add promotion to advertisement", description = "Adds a promotion to the specified advertisement")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added promotion to advertisement"),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdvertisementResponseDto> addPromotion(@RequestParam Long id, @RequestParam Long promotionId) {
        return ResponseEntity.ok(advertisementService.addPromotion(id, promotionId));
    }

    @DeleteMapping("/{id}/promotion")
    @Operation(summary = "Remove promotion from advertisement", description = "Removes the promotion from the specified advertisement")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully removed promotion from advertisement"),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdvertisementResponseDto> removePromotion(@PathVariable Long id) {
        return ResponseEntity.ok(advertisementService.removePromotion(id));
    }
}
