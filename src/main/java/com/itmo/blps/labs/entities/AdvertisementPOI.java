package com.itmo.blps.labs.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "advertisement_pois")
public class AdvertisementPOI {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "advertisement_id", nullable = false)
    private Advertisement advertisement;

    @ManyToOne
    @JoinColumn(name = "poi_id", nullable = false)
    private POI poi;

    @Column(nullable = false)
    private Double distanceInMeters;
}