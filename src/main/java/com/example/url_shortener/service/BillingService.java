package com.example.url_shortener.service;

import com.example.url_shortener.domain.Client;
import com.example.url_shortener.domain.Invoice;
import com.example.url_shortener.repository.ClientRepository;
import com.example.url_shortener.repository.InvoiceRepository;
import com.example.url_shortener.repository.UsageDailyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class BillingService {

    private final UsageDailyRepository usageDailyRepository;
    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;

    public BillingService(UsageDailyRepository usageDailyRepository,
                          ClientRepository clientRepository,
                          InvoiceRepository invoiceRepository) {
        this.usageDailyRepository = usageDailyRepository;
        this.clientRepository = clientRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public Invoice generateInvoice(Long clientId, LocalDate monthStart) {
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        // 1. Sum the client's visits for the month (single-table thanks to denormalized client_id).
        long totalVisits = usageDailyRepository.sumVisits(clientId, monthStart, monthEnd);

        // 2. Snapshot the CURRENT plan rate (frozen into the invoice).
        Client client = clientRepository.findById(clientId).orElseThrow();
        BigDecimal rate = client.getPlan().getRatePerVisit();

        // 3. amount = visits * rate, rounded to currency (2 dp).
        BigDecimal amount = rate.multiply(BigDecimal.valueOf(totalVisits))
                .setScale(2, RoundingMode.HALF_UP);

        // 4. Persist an immutable invoice with the snapshotted rate.
        Invoice invoice = new Invoice(clientId, monthStart, totalVisits, rate, amount);
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public List<Invoice> listInvoices(Long clientId) {
        return invoiceRepository.findByClientIdOrderByPeriodDesc(clientId);
    }
}