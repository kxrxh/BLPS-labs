package com.itmo.blps.lab1.entities;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@Builder
public class Position {
    @NotNull
    @Column(nullable = false)
    private Double latitude;

    @NotNull
    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;
}
