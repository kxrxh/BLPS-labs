package com.itmo.blps.labs.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.itmo.blps.labs.entities.RealEstateType;
import com.itmo.blps.labs.utils.RealEstateTypeDeserializer;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdDto {
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 100, message = "Title must be between 1 and 100 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;

    @Min(value = 0, message = "Price must be greater than 0")
    private double price;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotNull(message = "Real estate type is required")
    @JsonDeserialize(using = RealEstateTypeDeserializer.class)
    private RealEstateType realEstateType;
}
