package com.itmo.blps.lab1.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.services.core.LocationSearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/locations")
@Tag(name = "Location", description = "Location Search API")
@SecurityRequirement(name = "Bearer Authentication")
public class LocationController {

    @Autowired
    private LocationSearchService locationSearchService;

    @GetMapping("/nearby")
    @Operation(summary = "Find nearby advertisements", description = "Finds advertisements within default radius of given coordinates")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved nearby advertisements")
    public List<Advertisement> findNearbyAdvertisements(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        return locationSearchService.findNearbyAdvertisements(latitude, longitude);
    }

    @GetMapping("/nearby/custom")
    @Operation(summary = "Find nearby advertisements with custom radius", description = "Finds advertisements within specified radius of given coordinates")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved nearby advertisements")
    public List<Advertisement> findNearbyAdvertisementsWithRadius(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radiusInMeters) {
        return locationSearchService.findNearbyAdvertisements(latitude, longitude, radiusInMeters);
    }

    @GetMapping("/city/{city}")
    @Operation(summary = "Find advertisements by city", description = "Finds all advertisements in specified city")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved advertisements for the city")
    public List<Advertisement> findAdvertisementsByCity(@PathVariable String city) {
        return locationSearchService.findByLocation(city);
    }
}
