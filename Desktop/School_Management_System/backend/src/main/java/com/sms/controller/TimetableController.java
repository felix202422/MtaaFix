package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.entity.Timetable;
import com.sms.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Timetable>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(timetableService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Timetable>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.getById(id)));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<List<Timetable>>> getByClass(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.getByClass(classId)));
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<ApiResponse<List<Timetable>>> getByTeacher(@PathVariable Long teacherId) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.getByTeacher(teacherId)));
    }

    @GetMapping("/class/{classId}/day/{day}")
    public ResponseEntity<ApiResponse<List<Timetable>>> getByClassAndDay(@PathVariable Long classId, @PathVariable String day) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.getByClassAndDay(classId, day.toUpperCase())));
    }

    @GetMapping("/teacher/{teacherId}/day/{day}")
    public ResponseEntity<ApiResponse<List<Timetable>>> getByTeacherAndDay(@PathVariable Long teacherId, @PathVariable String day) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.getByTeacherAndDay(teacherId, day.toUpperCase())));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Timetable>> create(@RequestBody Timetable timetable) {
        return ResponseEntity.ok(ApiResponse.success("Timetable entry created", timetableService.create(timetable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Timetable>> update(@PathVariable Long id, @RequestBody Timetable timetable) {
        return ResponseEntity.ok(ApiResponse.success("Timetable entry updated", timetableService.update(id, timetable)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        timetableService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Timetable entry deleted", null));
    }

    public TimetableController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }
}
