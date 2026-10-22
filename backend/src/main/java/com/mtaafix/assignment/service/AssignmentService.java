package com.mtaafix.assignment.service;

import com.mtaafix.assignment.dto.AssignmentDto;
import com.mtaafix.common.response.PagedResponse;
import com.mtaafix.report.domain.Report;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.service.OrganisationMembershipService;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {

    private final ReportRepository reportRepository;
    private final OrganisationMembershipService membershipService;

    public AssignmentService(ReportRepository reportRepository,
            OrganisationMembershipService membershipService) {
        this.reportRepository = reportRepository;
        this.membershipService = membershipService;
    }

    /** Phase 17: paginated (NFR-7) — capped at size 100 by the controller. */
    @Transactional(readOnly = true)
    public PagedResponse<AssignmentDto> getAssignmentsByAssignee(String assigneeId,
            Pageable pageable) {
        Page<Report> page = reportRepository.findReportsByAssigneeId(assigneeId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AssignmentDto> getReportsByOrganisation(String organisationId, User user,
            Pageable pageable) {
        if (user.getRole() == User.Role.ORGANIZATION_ADMIN) {
            String ownOrgId = membershipService.orgIdOfOrNull(user);
            if (ownOrgId == null || !ownOrgId.equals(organisationId)) {
                throw new AccessDeniedException(
                        "Organisation assignments are outside your organisation scope.");
            }
        }
        Page<Report> page = reportRepository.findReportsByOrganisationIdWithAssignment(
                organisationId, pageable);
        return toPagedResponse(page);
    }

    private static PagedResponse<AssignmentDto> toPagedResponse(Page<Report> page) {
        // Reports without an assignment (organisation query) are not assignments.
        java.util.List<AssignmentDto> content = page.getContent().stream()
                .filter(r -> r.getAssignment() != null)
                .map(r -> AssignmentDto.from(r.getAssignment(), r))
                .filter(Objects::nonNull)
                .toList();
        return PagedResponse.of((int) page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize(), content);
    }
}
