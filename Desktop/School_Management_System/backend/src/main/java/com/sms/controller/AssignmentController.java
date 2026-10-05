package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.entity.Assignment;
import com.sms.entity.AssignmentSubmission;
import com.sms.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Assignment>>> getAllAssignments() {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.getAllAssignments()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Assignment>> getAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.getAssignmentById(id)));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<List<Assignment>>> getByClass(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.getByClass(classId)));
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<ApiResponse<List<Assignment>>> getByTeacher(@PathVariable Long teacherId) {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.getByTeacher(teacherId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Assignment>> createAssignment(@RequestBody Assignment assignment) {
        return ResponseEntity.ok(ApiResponse.success("Assignment created", assignmentService.createAssignment(assignment)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Assignment>> updateAssignment(@PathVariable Long id, @RequestBody Assignment assignment) {
        return ResponseEntity.ok(ApiResponse.success("Assignment updated", assignmentService.updateAssignment(id, assignment)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.ok(ApiResponse.success("Assignment deleted", null));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'STUDENT', 'REGISTRAR')")
    public ResponseEntity<ApiResponse<AssignmentSubmission>> submit(
            @PathVariable Long id, @RequestBody AssignmentSubmission submission) {
        return ResponseEntity.ok(ApiResponse.success("Assignment submitted", assignmentService.submitAssignment(id, submission)));
    }

    @GetMapping("/{id}/submissions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AssignmentSubmission>>> getSubmissions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.getSubmissions(id)));
    }

    @PostMapping("/submissions/{submissionId}/grade")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssignmentSubmission>> gradeSubmission(
            @PathVariable Long submissionId, @RequestBody AssignmentSubmission grading) {
        return ResponseEntity.ok(ApiResponse.success("Submission graded", assignmentService.gradeSubmission(submissionId, grading)));
    }

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }
}
