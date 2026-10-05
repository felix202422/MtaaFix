package com.sms.service;

import com.sms.entity.Invoice;
import com.sms.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface FeeService {
    Invoice createInvoice(Invoice invoice);
    Invoice getInvoiceById(Long id);
    List<Invoice> getInvoicesByStudent(Long studentId);
    Page<Invoice> getAllInvoices(Pageable pageable);
    Payment recordPayment(Payment payment);
    List<Payment> getPaymentsByStudent(Long studentId);
    List<Payment> getPaymentsByInvoice(Long invoiceId);
}
