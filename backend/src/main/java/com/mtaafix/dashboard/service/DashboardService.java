package com.mtaafix.dashboard.service;

import com.mtaafix.common.exceptions.ResourceNotFoundException;
import com.mtaafix.dashboard.dto.DashboardStatsDto;
import com.mtaafix.dashboard.dto.UserDashboardDto;
import com.mtaafix.report.domain.Report;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.user.domain.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DashboardService {

    private final ReportRepository reportRepository;

    public DashboardService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public UserDashboardDto getUserDashboard(User user) {
        long totalReports = reportRepository.countByUserId(user.getId());
        long submittedReports = reportRepository.countByUserIdAndStatus(user.getId(), Report.Status.SUBMITTED);
        long verifiedReports = reportRepository.countByUserIdAndStatus(user.getId(), Report.Status.VERIFIED);
        long closedReports = reportRepository.countByUserIdAndStatus(user.getId(), Report.Status.CLOSED);
        long needsInfoCount = reportRepository.countByUserIdAndStatus(user.getId(),
                Report.Status.NEEDS_INFORMATION);
        long resolvedAwaitingConfirmCount = reportRepository.countByUserIdAndStatus(user.getId(),
                Report.Status.RESOLVED);
        long activeCount = reportRepository.countActiveByUserId(user.getId());
        java.util.List<UserDashboardDto.AttentionReport> attention = reportRepository
                .findAttentionReportsByUserId(user.getId()).stream()
                .limit(5)
                .map(r -> new UserDashboardDto.AttentionReport(r.getId(), r.getCode(), r.getTitle(),
                        r.getStatus().name()))
                .toList();

        return new UserDashboardDto(
                initialsOf(user.getFullName()),
                user.getFullName(),
                user.getRole().name(),
                totalReports,
                submittedReports,
                verifiedReports,
                closedReports,
                needsInfoCount,
                resolvedAwaitingConfirmCount,
                activeCount,
                attention
        );
    }

    private static String initialsOf(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "MTF";
        }
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder("MTF");
        if (parts.length > 0 && !parts[0].isEmpty()) {
            sb.append(parts[0].charAt(0));
        }
        if (parts.length > 1 && !parts[parts.length - 1].isEmpty()) {
            sb.append(parts[parts.length - 1].charAt(0));
        }
        return sb.toString().toUpperCase();
    }

    public DashboardStatsDto getOrganizationStats(String orgId, User user) {
        if (user.getRole() == User.Role.ORGANIZATION_ADMIN
                && (user.getOrganisationId() == null
                        || !user.getOrganisationId().equals(orgId))) {
            throw new AccessDeniedException(
                    "Organisation statistics are outside your organisation scope.");
        }
        long totalReports = reportRepository.countByOrganisationId(orgId);
        long submittedReports = reportRepository.countByOrganisationIdAndStatus(orgId, Report.Status.SUBMITTED);
        long verifiedReports = reportRepository.countByOrganisationIdAndStatus(orgId, Report.Status.VERIFIED);
        long assignedReports = reportRepository.countByOrganisationIdAndStatus(orgId, Report.Status.ASSIGNED);

        return new DashboardStatsDto(
                orgId,
                Map.of(
                        "total", totalReports,
                        "submitted", submittedReports,
                        "verified", verifiedReports,
                        "assigned", assignedReports
                )
        );
    }

    public DashboardStatsDto getGlobalStats() {
        long totalReports = reportRepository.count();
        long submittedReports = reportRepository.countByStatus(Report.Status.SUBMITTED);
        long verifiedReports = reportRepository.countByStatus(Report.Status.VERIFIED);
        long closedReports = reportRepository.countByStatus(Report.Status.CLOSED);

        return new DashboardStatsDto(
                null,
                Map.of(
                        "total", totalReports,
                        "submitted", submittedReports,
                        "verified", verifiedReports,
                        "closed", closedReports
                )
        );
    }
}
