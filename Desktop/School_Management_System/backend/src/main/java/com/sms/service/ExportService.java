package com.sms.service;

import jakarta.servlet.http.HttpServletResponse;

public interface ExportService {
    void exportStudents(HttpServletResponse response, String format) throws Exception;
    void importStudents(org.springframework.web.multipart.MultipartFile file) throws Exception;
    void exportAttendance(HttpServletResponse response, String format) throws Exception;
    void exportPayments(HttpServletResponse response, String format) throws Exception;
    void exportExamResults(HttpServletResponse response, Long examinationId, String format) throws Exception;
    byte[] generateReportCardPdf(Long studentId, Long examinationId) throws Exception;
}
