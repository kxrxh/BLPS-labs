package com.itmo.blps.lab1.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NominatimResponse {
    private String lat;
    private String lon;
    private String displayName;
    private Double importance;
    private Long placeId;
}