package com.itmo.blps.lab1.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.itmo.blps.lab1.dto.PaymentDto;
import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.PaymentProvider;
import com.itmo.blps.lab1.security.UserAuthentication;
import com.itmo.blps.lab1.services.core.PaymentService;

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
@Tag(name = "Payment", description = "Payment Processing API")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/providers")
    @Operation(summary = "Get available payment providers", description = "Retrieves available payment providers from the database")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved payment providers")
    @PreAuthorize("isAuthenticated()")
    public List<PaymentProvider> getPaymentProviders() {
        return paymentService.getAvailableProviders();
    }

    @PostMapping("/process")
    @Operation(summary = "Process payment", description = "Processes payment for promotion and applies promotion if successful")
    @ApiResponse(responseCode = "200", description = "Returns the result of the payment process")
    @PreAuthorize("hasAuthority('payment:process')")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody @Valid PaymentDto paymentDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = ((UserAuthentication) userDetails).getUserId();

        Payment payment = paymentService.createAndProcessPayment(paymentDto, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("payment_id", payment.getId());

        return ResponseEntity.ok(response);
    }
}
