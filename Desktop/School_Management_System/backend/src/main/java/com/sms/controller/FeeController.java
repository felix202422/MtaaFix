package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.dto.response.PagedResponse;
import com.sms.entity.Invoice;
import com.sms.entity.Payment;
import com.sms.service.FeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/fees")
@RequiredArgsConstructor
public class FeeController {

    private final FeeService feeService;

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<PagedResponse<Invoice>>> getAllInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Invoice> invoices = feeService.getAllInvoices(pageable);
        PagedResponse<Invoice> response = PagedResponse.<Invoice>builder()
                .content(invoices.getContent()).page(invoices.getNumber()).size(invoices.getSize())
                .totalElements(invoices.getTotalElements()).totalPages(invoices.getTotalPages())
                .first(invoices.isFirst()).last(invoices.isLast()).build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/invoices/student/{studentId}")
    public ResponseEntity<ApiResponse<java.util.List<Invoice>>> getStudentInvoices(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(feeService.getInvoicesByStudent(studentId)));
    }

    @PostMapping("/invoices")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Invoice>> createInvoice(@Valid @RequestBody Invoice invoice) {
        return ResponseEntity.ok(ApiResponse.success("Invoice created", feeService.createInvoice(invoice)));
    }

    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Payment>> recordPayment(@Valid @RequestBody Payment payment) {
        return ResponseEntity.ok(ApiResponse.success("Payment recorded", feeService.recordPayment(payment)));
    }

    @GetMapping("/payments/student/{studentId}")
    public ResponseEntity<ApiResponse<java.util.List<Payment>>> getStudentPayments(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(feeService.getPaymentsByStudent(studentId)));
    }

    public FeeController(FeeService feeService) {
        this.feeService = feeService;
    }

}
