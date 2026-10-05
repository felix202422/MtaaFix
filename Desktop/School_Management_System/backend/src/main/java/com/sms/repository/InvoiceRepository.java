package com.sms.repository;

import com.sms.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByStudentId(Long studentId);

    List<Invoice> findByStatus(String status);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}
