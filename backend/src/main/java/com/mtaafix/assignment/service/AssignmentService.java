package com.mtaafix.assignment.service;

import com.mtaafix.assignment.dto.AssignmentDto;
import com.mtaafix.report.domain.Assignment;
import com.mtaafix.report.domain.Report;
import com.mtaafix.report.mapper.ReportMapper;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.service.OrganisationMembershipService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    private final ReportRepository reportRepository;
    private final OrganisationMembershipService membershipService;

    public AssignmentService(ReportRepository reportRepository,
            OrganisationMembershipService membershipService) {
        this.reportRepository = reportRepository;
        this.membershipService = membershipService;
    }

    public List<AssignmentDto> getAssignmentsByAssignee(String assigneeId) {
        return reportRepository.findReportsByAssigneeId(assigneeId).stream()
                .map(this::toAssignmentDto)
                .collect(Collectors.toList());
    }

    public List<AssignmentDto> getReportsByOrganisation(String organisationId, User user) {
        if (user.getRole() == User.Role.ORGANIZATION_ADMIN) {
            String ownOrgId = membershipService.orgIdOfOrNull(user);
            if (ownOrgId == null || !ownOrgId.equals(organisationId)) {
                throw new AccessDeniedException(
                        "Organisation assignments are outside your organisation scope.");
            }
        }
        return reportRepository.findReportsByOrganisationIdWithAssignment(organisationId).stream()
                .map(this::toAssignmentDto)
                .collect(Collectors.toList());
    }

    private AssignmentDto toAssignmentDto(Report report) {
        if (report.getAssignment() == null) {
            return null;
        }
        return AssignmentDto.from(report.getAssignment(), report);
    }
}
