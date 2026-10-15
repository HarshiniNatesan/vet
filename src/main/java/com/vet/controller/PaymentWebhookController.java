package com.vet.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/webhook")
public class PaymentWebhookController {
    @PostMapping public ResponseEntity<String> disabled(){return ResponseEntity.status(HttpStatus.GONE).body("Razorpay webhooks are disabled. UPI QR payments require manual verification.");}
}
