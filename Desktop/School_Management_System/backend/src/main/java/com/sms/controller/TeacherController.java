package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.dto.response.PagedResponse;
import com.sms.entity.Teacher;
import com.sms.service.TeacherService;
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
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'REGISTRAR')")
    public ResponseEntity<ApiResponse<PagedResponse<Teacher>>> getAllTeachers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Teacher> teachers = teacherService.getAllTeachers(pageable);
        PagedResponse<Teacher> response = PagedResponse.<Teacher>builder()
                .content(teachers.getContent())
                .page(teachers.getNumber())
                .size(teachers.getSize())
                .totalElements(teachers.getTotalElements())
                .totalPages(teachers.getTotalPages())
                .first(teachers.isFirst())
                .last(teachers.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Teacher>> getTeacherById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(teacherService.getTeacherById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Teacher>> createTeacher(@Valid @RequestBody Teacher teacher) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Teacher created successfully", teacherService.createTeacher(teacher)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Teacher>> updateTeacher(@PathVariable Long id, @Valid @RequestBody Teacher teacher) {
        return ResponseEntity.ok(ApiResponse.success("Teacher updated successfully", teacherService.updateTeacher(id, teacher)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.ok(ApiResponse.success("Teacher deleted successfully", null));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> getTeacherCount() {
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        counts.put("total", teacherService.getTeacherCount());
        return ResponseEntity.ok(ApiResponse.success(counts));
    }

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

}
