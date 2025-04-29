package com.itmo.blps.lab1.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "shedlock") // Explicitly map to the shedlock table
@Getter
@Setter
public class ShedLock {

    @Id
    @Column(name = "name", length = 64, nullable = false)
    private String name; // The unique name of the lock

    @Column(name = "lock_until", nullable = false, columnDefinition = "TIMESTAMP(3)")
    private LocalDateTime lockUntil; // Time until the lock is released

    @Column(name = "locked_at", nullable = false, columnDefinition = "TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3)")
    private LocalDateTime lockedAt; // Time when the lock was acquired

    @Column(name = "locked_by", length = 255, nullable = false)
    private String lockedBy; // Identifier of the node that holds the lock
} 