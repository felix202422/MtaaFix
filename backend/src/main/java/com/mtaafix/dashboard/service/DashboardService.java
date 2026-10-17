package com.mtaafix.dashboard.service;

import com.mtaafix.common.exceptions.ResourceNotFoundException;
import com.mtaafix.dashboard.dto.AdminDashboardDto;
import com.mtaafix.dashboard.dto.DashboardStatsDto;
import com.mtaafix.dashboard.dto.UserDashboardDto;
import com.mtaafix.report.domain.Report;
import com.mtaafix.report.domain.Report.Status;
import com.mtaafix.report.repository.OrganisationRepository;
import com.mtaafix.report.repository.ReportCategoryRepository;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
import com.mtaafix.user.service.OrganisationMembershipService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    /** Activity days are bucketed in Tanzanian local time (P14/P23). */
    static final ZoneId APP_ZONE = ZoneId.of("Africa/Dar_es_Salaam");

    private static final Set<Status> QUEUE_STATUSES =
            Set.of(Status.SUBMITTED, Status.UNDER_REVIEW, Status.RESOLUTION_REVIEW);
    private static final Set<Status> ACTIVE_WORK_STATUSES =
            Set.of(Status.ASSIGNED, Status.IN_PROGRESS);

    private final ReportRepository reportRepository;
    private final OrganisationMembershipService membershipService;
    private final ReportCategoryRepository categoryRepository;
    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;

    public DashboardService(ReportRepository reportRepository,
            OrganisationMembershipService membershipService,
            ReportCategoryRepository categoryRepository,
            OrganisationRepository organisationRepository,
            UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.membershipService = membershipService;
        this.categoryRepository = categoryRepository;
        this.organisationRepository = organisationRepository;
        this.userRepository = userRepository;
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
        if (user.getRole() == User.Role.ORGANIZATION_ADMIN) {
            String ownOrgId = membershipService.orgIdOfOrNull(user);
            if (ownOrgId == null || !ownOrgId.equals(orgId)) {
                throw new AccessDeniedException(
                        "Organisation statistics are outside your organisation scope.");
            }
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

    /** P14 admin dashboard: every figure derived from live queries (FR-7.4). */
    @Transactional(readOnly = true)
    public AdminDashboardDto getAdminDashboard() {
        Map<String, Long> byStatus = new HashMap<>();
        for (Status s : Status.values()) {
            byStatus.put(s.name(), 0L);
        }
        long total = 0;
        for (Object[] row : reportRepository.countGroupByStatus()) {
            Status status = (Status) row[0];
            long count = ((Number) row[1]).longValue();
            byStatus.put(status.name(), count);
            total += count;
        }
        long newLast7Days = reportRepository.countByReportedAtAfter(
                Instant.now().minus(Duration.ofDays(7)));

        List<AdminDashboardDto.DailyCount> activity = buildActivity14d();
        List<AdminDashboardDto.CategoryCount> byCategory = buildCategoryDistribution();
        AdminDashboardDto.ModerationQueue queue = buildModerationQueue();
        AdminDashboardDto.Workload workload = buildWorkload();
        AdminDashboardDto.Attention attention = new AdminDashboardDto.Attention(
                reportRepository.countByStatus(Status.NEEDS_INFORMATION),
                reportRepository.countByStatus(Status.RESOLVED),
                reportRepository.findTop5ByStatusOrderByReportedAtDesc(Status.NEEDS_INFORMATION)
                        .stream().map(DashboardService::toRecent).toList());
        List<AdminDashboardDto.RecentReport> recent =
                reportRepository.findRecent(PageRequest.of(0, 10)).stream()
                        .map(DashboardService::toRecent).toList();

        return new AdminDashboardDto(total, byStatus, newLast7Days, activity, byCategory,
                queue, workload, attention, recent);
    }

    private List<AdminDashboardDto.DailyCount> buildActivity14d() {
        Instant since = Instant.now().minus(Duration.ofDays(14));
        Map<LocalDate, Long> buckets = new HashMap<>();
        reportRepository.findReportedAtSince(since).forEach(instant -> {
            LocalDate day = instant.atZone(APP_ZONE).toLocalDate();
            buckets.merge(day, 1L, Long::sum);
        });
        List<AdminDashboardDto.DailyCount> series = new ArrayList<>();
        LocalDate today = LocalDate.now(APP_ZONE);
        for (int i = 13; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            series.add(new AdminDashboardDto.DailyCount(day, buckets.getOrDefault(day, 0L)));
        }
        return series;
    }

    private List<AdminDashboardDto.CategoryCount> buildCategoryDistribution() {
        List<Object[]> rows = reportRepository.countGroupByCategory();
        Set<String> ids = rows.stream()
                .map(row -> (String) row[0])
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> names = ids.isEmpty() ? Map.of()
                : categoryRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(c -> c.getId(),
                                c -> c.getName(), (a, b) -> a));
        return rows.stream()
                .sorted((a, b) -> Long.compare(((Number) b[1]).longValue(),
                        ((Number) a[1]).longValue()))
                .map(row -> new AdminDashboardDto.CategoryCount((String) row[0],
                        row[0] == null ? "Uncategorised"
                                : names.getOrDefault((String) row[0], "Unknown"),
                        ((Number) row[1]).longValue()))
                .toList();
    }

    private AdminDashboardDto.ModerationQueue buildModerationQueue() {
        List<AdminDashboardDto.RecentReport> items =
                reportRepository.findTop10ByStatusInOrderByReportedAtDesc(QUEUE_STATUSES)
                        .stream().map(DashboardService::toRecent).toList();
        return new AdminDashboardDto.ModerationQueue(
                reportRepository.countByStatus(Status.SUBMITTED),
                reportRepository.countByStatus(Status.UNDER_REVIEW),
                reportRepository.countByStatus(Status.NEEDS_INFORMATION),
                reportRepository.countByStatus(Status.RESOLUTION_REVIEW),
                items);
    }

    private AdminDashboardDto.Workload buildWorkload() {
        long unassigned = reportRepository.countByAssignmentIsNull();
        long assigned = reportRepository.countByAssignmentIsNotNull();

        List<Object[]> orgRows = reportRepository.countAssignedGroupByOrganisation();
        Set<String> orgIds = orgRows.stream()
                .map(row -> (String) row[0])
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> orgNames = orgIds.isEmpty() ? Map.of()
                : organisationRepository.findAllById(orgIds).stream()
                        .collect(Collectors.toMap(o -> o.getId(), o -> o.getName(), (a, b) -> a));
        List<AdminDashboardDto.OrgLoad> byOrg = orgRows.stream()
                .sorted((a, b) -> Long.compare(((Number) b[1]).longValue(),
                        ((Number) a[1]).longValue()))
                .limit(10)
                .map(row -> new AdminDashboardDto.OrgLoad((String) row[0],
                        row[0] == null ? "No organisation"
                                : orgNames.getOrDefault((String) row[0], "Unknown"),
                        ((Number) row[1]).longValue()))
                .toList();

        List<Object[]> workerRows = reportRepository.countActiveWorkGroupByAssignee(
                ACTIVE_WORK_STATUSES);
        Set<String> workerIds = workerRows.stream()
                .map(row -> (String) row[0])
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> workerNames = workerIds.isEmpty() ? Map.of()
                : userRepository.findAllById(workerIds).stream()
                        .collect(Collectors.toMap(u -> u.getId(), u -> u.getFullName(), (a, b) -> a));
        List<AdminDashboardDto.WorkerLoad> byWorker = workerRows.stream()
                .filter(row -> row[0] != null)
                .sorted((a, b) -> Long.compare(((Number) b[1]).longValue(),
                        ((Number) a[1]).longValue()))
                .limit(10)
                .map(row -> new AdminDashboardDto.WorkerLoad((String) row[0],
                        workerNames.getOrDefault((String) row[0], "Unknown"),
                        ((Number) row[1]).longValue()))
                .toList();

        return new AdminDashboardDto.Workload(unassigned, assigned, byOrg, byWorker);
    }

    static AdminDashboardDto.RecentReport toRecent(Report report) {
        Double lat = null;
        Double lng = null;
        if (report.getLocation() != null) {
            lat = report.getLocation().getY();
            lng = report.getLocation().getX();
        }
        return new AdminDashboardDto.RecentReport(
                report.getId(),
                report.getCode(),
                report.getTitle(),
                report.getStatus().name(),
                report.getCategory() != null ? report.getCategory().getId() : null,
                report.getOrganisation() != null ? report.getOrganisation().getId() : null,
                report.getReportedAt(),
                lat,
                lng);
    }
}
