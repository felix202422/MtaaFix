package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.dto.response.PagedResponse;
import com.sms.entity.Student;
import com.sms.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'REGISTRAR', 'TEACHER')")
    public ResponseEntity<ApiResponse<PagedResponse<Student>>> getAllStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Student> students = studentService.getAllStudents(pageable);
        PagedResponse<Student> response = PagedResponse.<Student>builder()
                .content(students.getContent())
                .page(students.getNumber())
                .size(students.getSize())
                .totalElements(students.getTotalElements())
                .totalPages(students.getTotalPages())
                .first(students.isFirst())
                .last(students.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'REGISTRAR', 'TEACHER')")
    public ResponseEntity<ApiResponse<Student>> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(studentService.getStudentById(id)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'REGISTRAR', 'TEACHER')")
    public ResponseEntity<ApiResponse<PagedResponse<Student>>> searchStudents(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Student> students = studentService.searchStudents(q, pageable);
        PagedResponse<Student> response = PagedResponse.<Student>builder()
                .content(students.getContent())
                .page(students.getNumber())
                .size(students.getSize())
                .totalElements(students.getTotalElements())
                .totalPages(students.getTotalPages())
                .first(students.isFirst())
                .last(students.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/by-class/{classId}")
    public ResponseEntity<ApiResponse<java.util.List<Student>>> getStudentsByClass(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success(studentService.getStudentsByClassId(classId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'REGISTRAR')")
    public ResponseEntity<ApiResponse<Student>> createStudent(@Valid @RequestBody Student student) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Student created successfully", studentService.createStudent(student)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'REGISTRAR')")
    public ResponseEntity<ApiResponse<Student>> updateStudent(@PathVariable Long id, @Valid @RequestBody Student student) {
        return ResponseEntity.ok(ApiResponse.success("Student updated successfully", studentService.updateStudent(id, student)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(ApiResponse.success("Student deleted successfully", null));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> getStudentCount() {
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        counts.put("total", studentService.getStudentCount());
        counts.put("active", studentService.getActiveStudentCount());
        return ResponseEntity.ok(ApiResponse.success(counts));
    }

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

}
