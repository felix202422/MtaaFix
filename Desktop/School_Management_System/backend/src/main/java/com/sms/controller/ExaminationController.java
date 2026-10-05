package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.dto.response.PagedResponse;
import com.sms.entity.Examination;
import com.sms.entity.ExamResult;
import com.sms.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/examinations")
@RequiredArgsConstructor
public class ExaminationController {

    private final ExamService examService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<Examination>>> getAllExams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Examination> exams = examService.getAllExaminations(pageable);
        PagedResponse<Examination> response = PagedResponse.<Examination>builder()
                .content(exams.getContent()).page(exams.getNumber()).size(exams.getSize())
                .totalElements(exams.getTotalElements()).totalPages(exams.getTotalPages())
                .first(exams.isFirst()).last(exams.isLast()).build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Examination>> getExamById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(examService.getExaminationById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Examination>> createExam(@Valid @RequestBody Examination exam) {
        return ResponseEntity.ok(ApiResponse.success("Examination created", examService.createExamination(exam)));
    }

    @PostMapping("/results")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ExamResult>> addResult(@Valid @RequestBody ExamResult result) {
        return ResponseEntity.ok(ApiResponse.success("Result added", examService.addResult(result)));
    }

    @GetMapping("/{examId}/results")
    public ResponseEntity<ApiResponse<List<ExamResult>>> getExamResults(@PathVariable Long examId) {
        return ResponseEntity.ok(ApiResponse.success(examService.getResultsByExam(examId)));
    }

    @GetMapping("/results/student/{studentId}")
    public ResponseEntity<ApiResponse<List<ExamResult>>> getStudentResults(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(examService.getResultsByStudent(studentId)));
    }

    @GetMapping("/term/{termId}")
    public ResponseEntity<ApiResponse<List<Examination>>> getExamsByTerm(@PathVariable Long termId) {
        return ResponseEntity.ok(ApiResponse.success(examService.getExaminationsByTerm(termId)));
    }

    public ExaminationController(ExamService examService) {
        this.examService = examService;
    }

}
