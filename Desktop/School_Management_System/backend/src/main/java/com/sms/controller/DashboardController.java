package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.security.CustomUserDetails;
import com.sms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminDashboard() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getAdminDashboard()));
    }

    @GetMapping("/teacher")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTeacherDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getTeacherDashboard(userDetails.getUser().getId())));
    }

    @GetMapping("/student")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStudentDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getStudentDashboard(userDetails.getUser().getId())));
    }

    @GetMapping("/parent")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getParentDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getParentDashboard(userDetails.getUser().getId())));
    }

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

}
