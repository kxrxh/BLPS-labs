package com.itmo.blps.labs.dto;

import jakarta.validation.constraints.Max;
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
    @Min(value = 1, message = "Advertisement ID must be greater than 0")
    private Long advertisementId;
    @NotNull
    @Min(value = 1, message = "Provider ID must be greater than 0")
    private Long providerId;
    @NotNull
    @Min(value = 1, message = "Amount must be greater than 0")
    @Max(value = 100000, message = "Amount must be less than 100000")
    private Double amount;
}
