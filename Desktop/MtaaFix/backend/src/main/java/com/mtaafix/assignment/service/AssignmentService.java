package com.mtaafix.assignment.service;

import com.mtaafix.assignment.dto.AssignmentDto;
import com.mtaafix.report.domain.Assignment;
import com.mtaafix.report.domain.Report;
import com.mtaafix.report.mapper.ReportMapper;
import com.mtaafix.report.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    private final ReportRepository reportRepository;

    public AssignmentService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public List<AssignmentDto> getAssignmentsByAssignee(String assigneeId) {
        return reportRepository.findReportsByAssigneeId(assigneeId).stream()
                .map(this::toAssignmentDto)
                .collect(Collectors.toList());
    }

    public List<AssignmentDto> getReportsByOrganisation(String organisationId) {
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
