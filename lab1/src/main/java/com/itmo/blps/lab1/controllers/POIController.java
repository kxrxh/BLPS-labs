package com.itmo.blps.lab1.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.itmo.blps.lab1.dto.PoiDto;
import com.itmo.blps.lab1.entities.POI;
import com.itmo.blps.lab1.services.core.POIService;

import io.basc.framework.lang.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/poi")
@Tag(name = "Points of Interest", description = "POI Management API")
@SecurityRequirement(name = "Bearer Authentication")
public class POIController {

    @Autowired
    private POIService poiService;

    @PostMapping("/")
    @Operation(summary = "Add POI", description = "Adds a new Point of Interest")
    @ApiResponse(responseCode = "200", description = "Successfully added POI")
    public void addPOI(@RequestBody @Valid PoiDto poiDto) {
        poiService.addPOI(poiDto);
    }

    @GetMapping("/")
    @Operation(summary = "Get all POIs", description = "Retrieves all Points of Interest")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all POIs")
    public ResponseEntity<List<POI>> getPOIs() {
        return ResponseEntity.ok(poiService.getPOIs());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get POI by ID", description = "Retrieves a Point of Interest by its ID")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved POI")
    @ApiResponse(responseCode = "404", description = "POI not found")
    public ResponseEntity<POI> getPOIById(@PathVariable Long id) {
        return ResponseEntity.ok(poiService.getPOIById(id).orElseThrow(() -> new NotFoundException("POI not found")));
    }

    @PostMapping("/update-advertisement-pois/{id}")
    @Operation(summary = "Update advertisement POIs", description = "Updates the Points of Interest associated with an advertisement based on its location")
    @ApiResponse(responseCode = "200", description = "Successfully updated advertisement POIs")
    @ApiResponse(responseCode = "404", description = "Advertisement not found")
    @ApiResponse(responseCode = "400", description = "Invalid advertisement ID")
    public void updateAdvertisementPOIs(@PathVariable Long id) {
        poiService.updateAdvertisementPOIs(id);
    }
}
