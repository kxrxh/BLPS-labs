package com.itmo.blps.lab1.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.services.core.POIService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/poi")
@Tag(name = "Points of Interest", description = "POI Management API")
public class POIController {

    @Autowired
    private POIService poiService;

    @PostMapping("/update-advertisement-pois")
    @Operation(summary = "Update advertisement POIs", description = "Updates the Points of Interest associated with an advertisement based on its location")
    @ApiResponse(responseCode = "200", description = "Successfully updated advertisement POIs")
    public void updateAdvertisementPOIs(@RequestBody Advertisement advertisement) {
        poiService.updateAdvertisementPOIs(advertisement);
    }
}
