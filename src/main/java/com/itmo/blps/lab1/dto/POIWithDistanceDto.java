package com.itmo.blps.lab1.dto;

import com.itmo.blps.lab1.entities.POI;
import com.itmo.blps.lab1.entities.AdvertisementPOI;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POIWithDistanceDto {
    private Long id;
    private String name;
    private String type;
    private Double latitude;
    private Double longitude;
    private String address;
    private String city;
    private Double distanceInMeters;

    public static POIWithDistanceDto fromEntity(AdvertisementPOI advertisementPOI) {
        POI poi = advertisementPOI.getPoi();
        return POIWithDistanceDto.builder()
                .id(poi.getId())
                .name(poi.getName())
                .type(poi.getType().name())
                .latitude(poi.getPosition().getLatitude())
                .longitude(poi.getPosition().getLongitude())
                .address(poi.getPosition().getAddress())
                .city(poi.getPosition().getCity())
                .distanceInMeters(advertisementPOI.getDistanceInMeters())
                .build();
    }
} 