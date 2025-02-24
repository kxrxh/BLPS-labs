package com.itmo.blps.lab1.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.itmo.blps.lab1.dto.PaymentDto;
import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.PaymentProvider;
import com.itmo.blps.lab1.services.core.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payment", description = "Payment Processing API")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Endpoint to get all available payment providers.
    @GetMapping("/providers")
    @Operation(summary = "Get available payment providers", description = "Retrieves available payment providers from the database")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved payment providers")
    public List<PaymentProvider> getAvailableProviders() {
        return paymentService.getAvailableProviders();
    }

    // Endpoint to process a payment.
    @PostMapping("/process")
    @Operation(summary = "Process payment", description = "Processes payment for promotion and applies promotion if successful")
    @ApiResponse(responseCode = "200", description = "Returns the result of the payment process")
    public String processPayment(@RequestBody PaymentDto paymentDto) {
        Payment payment = paymentService.createPayment(paymentDto);
        return paymentService.processPayment(payment);
    }
}
