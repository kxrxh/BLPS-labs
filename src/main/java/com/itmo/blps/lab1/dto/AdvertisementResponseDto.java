package com.itmo.blps.lab1.dto;

import com.itmo.blps.lab1.entities.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementResponseDto {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private User author;
    private Position position;
    private RealEstateType realEstateType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
    private Boolean isPromoted;
    private Promotion promotion;
    private LocalDateTime startDate;
    private Integer durationInDays;
    private List<POIWithDistanceDto> pois;

    public static AdvertisementResponseDto fromEntity(Advertisement advertisement, List<AdvertisementPOI> pois) {
        return AdvertisementResponseDto.builder()
                .id(advertisement.getId())
                .name(advertisement.getName())
                .description(advertisement.getDescription())
                .price(advertisement.getPrice())
                .author(advertisement.getAuthor())
                .position(advertisement.getPosition())
                .realEstateType(advertisement.getRealEstateType())
                .createdAt(advertisement.getCreatedAt())
                .updatedAt(advertisement.getUpdatedAt())
                .isActive(advertisement.getIsActive())
                .isPromoted(advertisement.getIsPromoted())
                .promotion(advertisement.getPromotion())
                .startDate(advertisement.getStartDate())
                .durationInDays(advertisement.getDurationInDays())
                .pois(pois.stream()
                        .map(POIWithDistanceDto::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }
} 