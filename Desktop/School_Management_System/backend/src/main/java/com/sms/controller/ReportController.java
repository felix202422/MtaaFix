package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ExportService exportService;

    @GetMapping("/students/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'REGISTRAR', 'TEACHER')")
    public void exportStudents(HttpServletResponse response,
                               @RequestParam(defaultValue = "csv") String format) throws Exception {
        exportService.exportStudents(response, format);
    }

    @PostMapping("/students/import")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'REGISTRAR')")
    public ResponseEntity<ApiResponse<Void>> importStudents(@RequestParam("file") MultipartFile file) throws Exception {
        exportService.importStudents(file);
        return ResponseEntity.ok(ApiResponse.success("Students imported successfully", null));
    }

    @GetMapping("/attendance/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public void exportAttendance(HttpServletResponse response,
                                 @RequestParam(defaultValue = "csv") String format) throws Exception {
        exportService.exportAttendance(response, format);
    }

    @GetMapping("/payments/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public void exportPayments(HttpServletResponse response,
                               @RequestParam(defaultValue = "csv") String format) throws Exception {
        exportService.exportPayments(response, format);
    }

    @GetMapping("/examinations/{examinationId}/results/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public void exportExamResults(HttpServletResponse response,
                                  @PathVariable Long examinationId,
                                  @RequestParam(defaultValue = "csv") String format) throws Exception {
        exportService.exportExamResults(response, examinationId, format);
    }

    @GetMapping("/report-card")
    public ResponseEntity<byte[]> reportCard(@RequestParam Long studentId,
                                             @RequestParam Long examinationId) throws Exception {
        byte[] pdf = exportService.generateReportCardPdf(studentId, examinationId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=report-card-" + studentId + "-" + examinationId + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    public ReportController(ExportService exportService) {
        this.exportService = exportService;
    }
}
