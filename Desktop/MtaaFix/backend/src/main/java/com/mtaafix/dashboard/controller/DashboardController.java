package com.mtaafix.dashboard.controller;

import com.mtaafix.common.response.SimpleResponse;
import com.mtaafix.dashboard.dto.DashboardStatsDto;
import com.mtaafix.dashboard.dto.UserDashboardDto;
import com.mtaafix.dashboard.service.DashboardService;
import com.mtaafix.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "Dashboard endpoints for user statistics and overview")
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Get current user's dashboard summary")
    @GetMapping("/me")
    public ResponseEntity<UserDashboardDto> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getUserDashboard(user));
    }

    @Operation(summary = "Get organization statistics")
    @GetMapping("/organizations/{id}/stats")
    @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<DashboardStatsDto> orgStats(@PathVariable String id) {
        return ResponseEntity.ok(dashboardService.getOrganizationStats(id));
    }

    @Operation(summary = "Get global statistics")
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> stats() {
        return ResponseEntity.ok(dashboardService.getGlobalStats());
    }
}
