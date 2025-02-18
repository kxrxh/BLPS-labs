package com.itmo.blps.lab1.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itmo.blps.lab1.dto.AdDto;
import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.services.core.AdvertisementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/advertisements")
@Tag(name = "Advertisement", description = "Advertisement API")
public class AdvertisementController {

    @Autowired
    private AdvertisementService advertisementService;

    @PostMapping("/create")
    @Operation(summary = "Create a new advertisement", description = "Creates a new advertisement with the provided details")
    @ApiResponse(responseCode = "200", description = "Advertisement created successfully")
    public Advertisement createAdvertisement(@RequestBody @Valid AdDto adDto) {
        return advertisementService.createAdvertisement(adDto);
    }

}
