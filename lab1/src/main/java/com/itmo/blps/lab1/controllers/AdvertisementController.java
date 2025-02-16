package com.itmo.blps.lab1.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itmo.blps.lab1.services.core.AdvertisementService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/advertisements")
@Tag(name = "Advertisement", description = "Advertisement API")
public class AdvertisementController {

    @Autowired
    private AdvertisementService advertisementService;

}
