package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.dto.response.PagedResponse;
import com.sms.entity.AuditLog;
import com.sms.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLog>>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> logs = auditLogRepository.findAll(pageable);
        PagedResponse<AuditLog> response = PagedResponse.<AuditLog>builder()
                .content(logs.getContent()).page(logs.getNumber()).size(logs.getSize())
                .totalElements(logs.getTotalElements()).totalPages(logs.getTotalPages())
                .first(logs.isFirst()).last(logs.isLast()).build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId)));
    }

    @GetMapping("/action/{action}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getByAction(@PathVariable String action) {
        return ResponseEntity.ok(ApiResponse.success(auditLogRepository.findByAction(action)));
    }

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

}
