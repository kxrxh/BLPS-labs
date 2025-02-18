package com.itmo.blps.lab1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NominatimResponse {
    private String lat;
    private String lon;
    private String type;
    private Integer place_rank;
    private Double importance;
    private String name;
    private String display_name;
} 