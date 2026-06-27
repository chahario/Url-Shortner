package com.example.url_shortener.repository;

import com.example.url_shortener.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByClientIdOrderByPeriodDesc(Long clientId);
}