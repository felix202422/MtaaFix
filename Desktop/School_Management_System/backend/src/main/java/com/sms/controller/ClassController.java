package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.dto.response.PagedResponse;
import com.sms.entity.ClassRoom;
import com.sms.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<ClassRoom>>> getAllClasses() {
        return ResponseEntity.ok(ApiResponse.success(classService.getAllClasses()));
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PagedResponse<ClassRoom>>> getClassesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ClassRoom> classes = classService.getClassesPaged(pageable);
        PagedResponse<ClassRoom> response = PagedResponse.<ClassRoom>builder()
                .content(classes.getContent())
                .page(classes.getNumber()).size(classes.getSize())
                .totalElements(classes.getTotalElements())
                .totalPages(classes.getTotalPages())
                .first(classes.isFirst()).last(classes.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassRoom>> getClassById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(classService.getClassById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<ClassRoom>> createClass(@Valid @RequestBody ClassRoom classRoom) {
        return ResponseEntity.ok(ApiResponse.success("Class created successfully", classService.createClass(classRoom)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<ClassRoom>> updateClass(@PathVariable Long id, @Valid @RequestBody ClassRoom classRoom) {
        return ResponseEntity.ok(ApiResponse.success("Class updated successfully", classService.updateClass(id, classRoom)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteClass(@PathVariable Long id) {
        classService.deleteClass(id);
        return ResponseEntity.ok(ApiResponse.success("Class deleted successfully", null));
    }

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

}
