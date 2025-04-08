package com.itmo.blps.lab1.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.dto.AdvertisementResponseDto;
import com.itmo.blps.lab1.services.core.LocationSearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/locations")
@Tag(name = "Location", description = "Location-based Search API")
@SecurityRequirement(name = "Bearer Authentication")
public class LocationController {

    @Autowired
    private LocationSearchService locationSearchService;

    @GetMapping("/nearby")
    @Operation(summary = "Find nearby advertisements", description = "Finds advertisements within a radius of the given coordinates")
    @PreAuthorize("hasAuthority('location:read')") // USER, MODERATOR, ADMIN
    public List<AdvertisementResponseDto> findNearbyAdvertisements(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) Double radiusInMeters) {
        
        List<Advertisement> advertisements;
        if (radiusInMeters != null) {
            advertisements = locationSearchService.findNearbyAdvertisements(latitude, longitude, radiusInMeters);
        } else {
            advertisements = locationSearchService.findNearbyAdvertisements(latitude, longitude);
        }
        return advertisements.stream()
               .map(ad -> AdvertisementResponseDto.fromEntity(ad, List.of()))
               .collect(Collectors.toList());
    }

    @GetMapping("/city/{city}")
    @Operation(summary = "Find advertisements by city", description = "Finds advertisements located in the specified city")
    @PreAuthorize("hasAuthority('location:read')") // USER, MODERATOR, ADMIN
    public List<AdvertisementResponseDto> findAdvertisementsByCity(@PathVariable String city) {
         List<Advertisement> advertisements = locationSearchService.findByLocation(city);
         // Convert to DTOs
         return advertisements.stream()
               .map(ad -> AdvertisementResponseDto.fromEntity(ad, List.of()))
               .collect(Collectors.toList());
    }
}
