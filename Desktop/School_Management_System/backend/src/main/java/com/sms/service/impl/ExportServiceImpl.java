package com.sms.service.impl;

import com.sms.entity.Attendance;
import com.sms.entity.ExamResult;
import com.sms.entity.Payment;
import com.sms.entity.Student;
import com.sms.exception.BadRequestException;
import com.sms.exception.ResourceNotFoundException;
import com.sms.repository.AttendanceRepository;
import com.sms.repository.ExamResultRepository;
import com.sms.repository.ExaminationRepository;
import com.sms.repository.PaymentRepository;
import com.sms.repository.StudentRepository;
import com.sms.service.ExportService;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExportServiceImpl implements ExportService {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final PaymentRepository paymentRepository;
    private final ExamResultRepository examResultRepository;
    private final ExaminationRepository examinationRepository;

    public ExportServiceImpl(StudentRepository studentRepository,
                             AttendanceRepository attendanceRepository,
                             PaymentRepository paymentRepository,
                             ExamResultRepository examResultRepository,
                             ExaminationRepository examinationRepository) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.paymentRepository = paymentRepository;
        this.examResultRepository = examResultRepository;
        this.examinationRepository = examinationRepository;
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ============================== Students ================================

    @Override
    public void exportStudents(HttpServletResponse response, String format) throws Exception {
        List<Student> students = studentRepository.findAll();
        String[] header = {"ID", "Admission Number", "Full Name", "Gender", "Date of Birth", "Class", "Status"};
        List<String[]> rows = new ArrayList<>();
        for (Student s : students) {
            rows.add(new String[]{
                    String.valueOf(s.getId()),
                    s.getAdmissionNumber(),
                    s.getUser() != null ? s.getUser().getFullName() : "",
                    s.getGender() != null ? s.getGender() : "",
                    s.getDateOfBirth() != null ? s.getDateOfBirth().format(DATE_FMT) : "",
                    s.getCurrentClass() != null ? s.getCurrentClass().getName() : "",
                    s.getAcademicStatus() != null ? s.getAcademicStatus() : ""
            });
        }
        if ("pdf".equalsIgnoreCase(format)) {
            writePdf(response, "students.pdf", "Students", header, rows);
        } else if ("excel".equalsIgnoreCase(format)) {
            writeExcel(response, "students.xlsx", "Students", header, rows);
        } else {
            writeCsv(response, "students.csv", header, rows);
        }
    }

    @Override
    public void importStudents(MultipartFile file) throws Exception {
        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            rows = reader.readAll();
        }
        if (rows.size() <= 1) {
            throw new BadRequestException("CSV file has no data rows");
        }
        for (int i = 1; i < rows.size(); i++) {
            String[] r = rows.get(i);
            if (r.length < 7) {
                continue;
            }
            Student s = new Student();
            s.setAdmissionNumber(r[1].trim());
            s.setGender(r[3].trim());
            if (!r[4].isBlank()) {
                s.setDateOfBirth(LocalDate.parse(r[4].trim()));
            }
            s.setAcademicStatus(r[6].isBlank() ? "ACTIVE" : r[6].trim());
            studentRepository.save(s);
        }
    }

    // ============================== Attendance ===============================

    @Override
    public void exportAttendance(HttpServletResponse response, String format) throws Exception {
        List<Attendance> records = attendanceRepository.findAll();
        String[] header = {"ID", "Student", "Class", "Date", "Status"};
        List<String[]> rows = new ArrayList<>();
        for (Attendance a : records) {
            rows.add(new String[]{
                    String.valueOf(a.getId()),
                    a.getStudent() != null ? a.getStudent().getAdmissionNumber() : "",
                    a.getClassRoom() != null ? a.getClassRoom().getName() : "",
                    a.getDate() != null ? a.getDate().format(DATE_FMT) : "",
                    a.getStatus() != null ? a.getStatus() : ""
            });
        }
        if ("pdf".equalsIgnoreCase(format)) {
            writePdf(response, "attendance.pdf", "Attendance", header, rows);
        } else if ("excel".equalsIgnoreCase(format)) {
            writeExcel(response, "attendance.xlsx", "Attendance", header, rows);
        } else {
            writeCsv(response, "attendance.csv", header, rows);
        }
    }

    // ============================== Payments ================================

    @Override
    public void exportPayments(HttpServletResponse response, String format) throws Exception {
        List<Payment> payments = paymentRepository.findAll();
        String[] header = {"ID", "Payment Number", "Student", "Amount", "Method", "Date"};
        List<String[]> rows = new ArrayList<>();
        for (Payment p : payments) {
            rows.add(new String[]{
                    String.valueOf(p.getId()),
                    p.getPaymentNumber(),
                    p.getStudent() != null ? p.getStudent().getAdmissionNumber() : "",
                    p.getAmount() != null ? p.getAmount().toPlainString() : "",
                    p.getPaymentMethod() != null ? p.getPaymentMethod() : "",
                    p.getPaymentDate() != null ? p.getPaymentDate().format(DATE_FMT) : ""
            });
        }
        if ("pdf".equalsIgnoreCase(format)) {
            writePdf(response, "payments.pdf", "Payments", header, rows);
        } else if ("excel".equalsIgnoreCase(format)) {
            writeExcel(response, "payments.xlsx", "Payments", header, rows);
        } else {
            writeCsv(response, "payments.csv", header, rows);
        }
    }

    // ============================== Exam Results =============================

    @Override
    public void exportExamResults(HttpServletResponse response, Long examinationId, String format) throws Exception {
        List<ExamResult> results = examResultRepository.findByExaminationId(examinationId);
        String[] header = {"Student", "Subject", "Marks", "Grade"};
        List<String[]> rows = new ArrayList<>();
        for (ExamResult r : results) {
            rows.add(new String[]{
                    r.getStudent() != null ? r.getStudent().getAdmissionNumber() : "",
                    r.getSubject() != null ? r.getSubject().getName() : "",
                    r.getMarksObtained() != null ? r.getMarksObtained().toPlainString() : "",
                    r.getGrade() != null ? r.getGrade() : ""
            });
        }
        String base = "exam-" + examinationId + "-results";
        if ("pdf".equalsIgnoreCase(format)) {
            writePdf(response, base + ".pdf", "Examination Results", header, rows);
        } else if ("excel".equalsIgnoreCase(format)) {
            writeExcel(response, base + ".xlsx", "Results", header, rows);
        } else {
            writeCsv(response, base + ".csv", header, rows);
        }
    }

    // ============================== Report Card PDF ==========================

    @Override
    public byte[] generateReportCardPdf(Long studentId, Long examinationId) throws Exception {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        List<ExamResult> results = examResultRepository.findByExaminationIdAndStudentId(examinationId, studentId);

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(out);
        com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);

        com.itextpdf.kernel.font.PdfFont bold = com.itextpdf.kernel.font.PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);

        document.add(new com.itextpdf.layout.element.Paragraph("Report Card")
                .setFont(bold).setFontSize(18).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        document.add(new com.itextpdf.layout.element.Paragraph(
                student.getUser() != null ? student.getUser().getFullName() : student.getAdmissionNumber())
                .setFontSize(12).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        document.add(new com.itextpdf.layout.element.Paragraph("Admission No: " + student.getAdmissionNumber())
                .setFontSize(10).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(12));

        com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(
                new float[]{4, 2, 2, 2}).useAllAvailableWidth();
        String[] headers = {"Subject", "Marks", "Grade", "Remarks"};
        for (String h : headers) {
            table.addHeaderCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(h).setFont(bold).setFontSize(10)));
        }
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        int count = 0;
        for (ExamResult r : results) {
            String marks = r.getMarksObtained() != null ? r.getMarksObtained().toPlainString() : "-";
            if (r.getMarksObtained() != null) {
                total = total.add(r.getMarksObtained());
                count++;
            }
            table.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(
                            r.getSubject() != null ? r.getSubject().getName() : "-").setFontSize(9)));
            table.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(marks).setFontSize(9)));
            table.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(
                            r.getGrade() != null ? r.getGrade() : "-").setFontSize(9)));
            table.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(
                            r.getRemarks() != null ? r.getRemarks() : "").setFontSize(9)));
        }
        document.add(table);

        if (count > 0) {
            java.math.BigDecimal avg = total.divide(java.math.BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);
            document.add(new com.itextpdf.layout.element.Paragraph("Average: " + avg.toPlainString())
                    .setFont(bold).setFontSize(11).setMarginTop(12));
        }
        document.close();
        return out.toByteArray();
    }

    // ============================== Writers ==================================

    private void writeCsv(HttpServletResponse response, String filename, String[] header, List<String[]> rows) throws Exception {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        try (CSVWriter writer = new CSVWriter(response.getWriter())) {
            writer.writeNext(header);
            for (String[] row : rows) {
                writer.writeNext(row);
            }
        }
    }

    private void writeExcel(HttpServletResponse response, String filename, String sheetName, String[] header, List<String[]> rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(sheetName);
            Row head = sheet.createRow(0);
            for (int i = 0; i < header.length; i++) {
                head.createCell(i).setCellValue(header[i]);
            }
            int rowNum = 1;
            for (String[] row : rows) {
                Row dataRow = sheet.createRow(rowNum++);
                for (int i = 0; i < row.length; i++) {
                    dataRow.createCell(i).setCellValue(row[i] != null ? row[i] : "");
                }
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            workbook.write(response.getOutputStream());
        }
    }

    private void writePdf(HttpServletResponse response, String filename, String title, String[] header, List<String[]> rows) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(out);
        com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);
        com.itextpdf.kernel.font.PdfFont bold = com.itextpdf.kernel.font.PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);

        document.add(new com.itextpdf.layout.element.Paragraph(title)
                .setFont(bold).setFontSize(16)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(12));

        float[] widths = new float[header.length];
        java.util.Arrays.fill(widths, 1f);
        com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(widths).useAllAvailableWidth();
        for (String h : header) {
            table.addHeaderCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(h).setFont(bold).setFontSize(9)));
        }
        for (String[] row : rows) {
            for (String cell : row) {
                table.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(cell != null ? cell : "").setFontSize(8)));
            }
        }
        document.add(table);
        document.close();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.getOutputStream().write(out.toByteArray());
    }
}
