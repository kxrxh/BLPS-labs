package com.itmo.blps.lab1.entities;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Chosen payment provider (the user selects one from the available providers)
    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private PaymentProvider provider;

    // The promotion being paid for
    @OneToOne
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    // Payment amount (could be derived from the promotion price)
    @Column(nullable = false)
    private Double amount;

    // The user who is paying for the promotion
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User payer;

    // Payment status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
