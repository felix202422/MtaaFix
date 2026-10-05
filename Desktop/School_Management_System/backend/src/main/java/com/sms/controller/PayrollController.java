package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.dto.response.PagedResponse;
import com.sms.entity.Payroll;
import com.sms.service.PayrollService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<Payroll>>> getAllPayroll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Payroll> records = payrollService.getAllPayroll(pageable);
        PagedResponse<Payroll> response = PagedResponse.<Payroll>builder()
                .content(records.getContent()).page(records.getNumber()).size(records.getSize())
                .totalElements(records.getTotalElements()).totalPages(records.getTotalPages())
                .first(records.isFirst()).last(records.isLast()).build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Payroll>> getPayroll(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayrollById(id)));
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<ApiResponse<List<Payroll>>> getByTeacher(@PathVariable Long teacherId) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getByTeacher(teacherId)));
    }

    @GetMapping("/month/{month}/year/{year}")
    public ResponseEntity<ApiResponse<List<Payroll>>> getByMonthAndYear(@PathVariable Integer month, @PathVariable Integer year) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getByMonthAndYear(month, year)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Payroll>> addPayroll(@Valid @RequestBody Payroll payroll) {
        return ResponseEntity.ok(ApiResponse.success("Payroll created", payrollService.addPayroll(payroll)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Payroll>> updatePayroll(@PathVariable Long id, @Valid @RequestBody Payroll payroll) {
        return ResponseEntity.ok(ApiResponse.success("Payroll updated", payrollService.updatePayroll(id, payroll)));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Payroll>> processPayment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payment processed", payrollService.processPayment(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Void>> deletePayroll(@PathVariable Long id) {
        payrollService.deletePayroll(id);
        return ResponseEntity.ok(ApiResponse.success("Payroll deleted", null));
    }

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

}
