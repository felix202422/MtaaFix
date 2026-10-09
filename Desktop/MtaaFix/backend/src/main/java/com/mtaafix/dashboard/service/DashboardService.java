package com.mtaafix.dashboard.service;

import com.mtaafix.common.exceptions.ResourceNotFoundException;
import com.mtaafix.dashboard.dto.DashboardStatsDto;
import com.mtaafix.dashboard.dto.UserDashboardDto;
import com.mtaafix.report.domain.Report;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.user.domain.User;
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

        return new UserDashboardDto(
                "MTF-" + user.getId().substring(0, 8).toUpperCase(),
                user.getFullName(),
                user.getRole().name(),
                totalReports,
                submittedReports,
                verifiedReports,
                closedReports
        );
    }

    public DashboardStatsDto getOrganizationStats(String orgId) {
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
