package com.itmo.blps.lab1.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDto {
    @NotNull
    @Min(value = 1, message = "Promotion ID must be greater than 0")
    private Long promotionId;
    @NotNull
    @Min(value = 1, message = "Provider ID must be greater than 0")
    private Long providerId;
    @NotNull
    @Min(value = 1, message = "Amount must be greater than 0")
    private Double amount;
}
