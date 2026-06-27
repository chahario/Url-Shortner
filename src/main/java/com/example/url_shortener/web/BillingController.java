package com.example.url_shortener.web;

import com.example.url_shortener.domain.Invoice;
import com.example.url_shortener.security.CurrentClient;
import com.example.url_shortener.service.BillingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    // Generate an invoice for the calling client for a given month (e.g. ?month=2026-06).
    @PostMapping("/invoices")
    public ResponseEntity<Invoice> generate(@RequestParam String month) {
        Long clientId = CurrentClient.get().getClientId();
        LocalDate monthStart = LocalDate.parse(month + "-01");
        Invoice invoice = billingService.generateInvoice(clientId, monthStart);
        return ResponseEntity.ok(invoice);
    }

    // List the calling client's invoices.
    @GetMapping("/invoices")
    public ResponseEntity<List<Invoice>> list() {
        Long clientId = CurrentClient.get().getClientId();
        return ResponseEntity.ok(billingService.listInvoices(clientId));
    }
}