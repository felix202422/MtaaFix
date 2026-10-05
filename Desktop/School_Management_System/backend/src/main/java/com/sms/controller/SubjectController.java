package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.entity.Subject;
import com.sms.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectRepository subjectRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Subject>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(subjectRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Subject>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(subjectRepository.findById(id)
                .orElseThrow(() -> new com.sms.exception.ResourceNotFoundException("Subject not found with id: " + id))));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<ApiResponse<List<Subject>>> getByDepartment(@PathVariable Long departmentId) {
        return ResponseEntity.ok(ApiResponse.success(subjectRepository.findByDepartmentId(departmentId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Subject>> create(@RequestBody Subject subject) {
        return ResponseEntity.ok(ApiResponse.success("Subject created", subjectRepository.save(subject)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Subject>> update(@PathVariable Long id, @RequestBody Subject subject) {
        Subject existing = subjectRepository.findById(id)
                .orElseThrow(() -> new com.sms.exception.ResourceNotFoundException("Subject not found with id: " + id));
        existing.setName(subject.getName());
        existing.setCode(subject.getCode());
        existing.setDepartment(subject.getDepartment());
        existing.setDescription(subject.getDescription());
        existing.setIsCompulsory(subject.getIsCompulsory());
        return ResponseEntity.ok(ApiResponse.success("Subject updated", subjectRepository.save(existing)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        subjectRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Subject deleted", null));
    }

    public SubjectController(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }
}
