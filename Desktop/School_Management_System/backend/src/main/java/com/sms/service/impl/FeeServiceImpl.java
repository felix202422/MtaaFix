package com.sms.service.impl;

import com.sms.entity.Invoice;
import com.sms.entity.Payment;
import com.sms.exception.ResourceNotFoundException;
import com.sms.repository.InvoiceRepository;
import com.sms.repository.PaymentRepository;
import com.sms.service.FeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FeeServiceImpl implements FeeService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public Invoice createInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByStudent(Long studentId) {
        return invoiceRepository.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Invoice> getAllInvoices(Pageable pageable) {
        return invoiceRepository.findAll(pageable);
    }

    @Override
    public Payment recordPayment(Payment payment) {
        Payment saved = paymentRepository.save(payment);
        Invoice invoice = payment.getInvoice();
        if (invoice != null) {
            double paidSoFar = paymentRepository.findByInvoiceId(invoice.getId())
                    .stream().mapToDouble(p -> p.getAmount().doubleValue()).sum();
            invoice.setPaidAmount(java.math.BigDecimal.valueOf(paidSoFar));
            if (invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
                invoice.setStatus("PAID");
            } else if (invoice.getPaidAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                invoice.setStatus("PARTIAL");
            }
            invoiceRepository.save(invoice);
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByStudent(Long studentId) {
        return paymentRepository.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByInvoice(Long invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId);
    }

    public FeeServiceImpl(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

}
