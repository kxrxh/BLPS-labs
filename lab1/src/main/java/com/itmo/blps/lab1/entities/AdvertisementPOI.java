package com.itmo.blps.lab1.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
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