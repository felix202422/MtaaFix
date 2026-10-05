package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.entity.Attendance;
import com.sms.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Attendance>> markAttendance(@RequestBody Attendance attendance) {
        return ResponseEntity.ok(ApiResponse.success("Attendance marked", attendanceService.markAttendance(attendance)));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<Attendance>>> markBulkAttendance(@RequestBody List<Attendance> attendances) {
        return ResponseEntity.ok(ApiResponse.success("Attendance marked", attendanceService.markBulkAttendance(attendances)));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<Attendance>>> getStudentAttendance(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getAttendanceByStudentAndDateRange(studentId, start, end)));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<List<Attendance>>> getClassAttendance(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getAttendanceByClassAndDate(classId, date)));
    }

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

}
