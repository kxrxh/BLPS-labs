package com.itmo.blps.labs.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itmo.blps.labs.dto.PaymentDto;
import com.itmo.blps.labs.entities.Payment;
import com.itmo.blps.labs.entities.PaymentProvider;
import com.itmo.blps.labs.services.core.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "Payment Management API")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MODERATOR')")
    @Operation(summary = "Create payment", description = "Creates a new payment")
    @ApiResponse(responseCode = "200", description = "Successfully created payment")
    public ResponseEntity<Payment> createPayment(@RequestBody @Valid PaymentDto paymentDto) {
        return ResponseEntity.ok(paymentService.createPayment(paymentDto));
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get all payments", description = "Retrieves all payments")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all payments")
    public ResponseEntity<List<Payment>> getPayments() {
        return ResponseEntity.ok(paymentService.getPayments());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or @paymentService.isPaymentOwner(#id, authentication.principal)")
    @Operation(summary = "Get payment by ID", description = "Retrieves a payment by its ID")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved payment")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/providers")
    @Operation(summary = "Get available payment providers", description = "Retrieves available payment providers from the database")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved payment providers")
    public List<PaymentProvider> getAvailableProviders() {
        return paymentService.getAvailableProviders();
    }

    @PostMapping("/process")
    @Operation(summary = "Process payment", description = "Processes payment for promotion and applies promotion if successful")
    @ApiResponse(responseCode = "200", description = "Returns the result of the payment process")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody @Valid PaymentDto paymentDto) {
        Payment payment = paymentService.createPayment(paymentDto);
        String paymentInfo = paymentService.processPayment(payment);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("payment_info", paymentInfo);

        return ResponseEntity.ok(response);
    }
}
