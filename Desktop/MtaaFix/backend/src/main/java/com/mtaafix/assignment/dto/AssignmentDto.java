package com.mtaafix.assignment.dto;

import com.mtaafix.report.domain.Assignment;
import com.mtaafix.report.domain.Report;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Assignment representation")
public record AssignmentDto(
        @Schema(description = "Assignment ID") String id,
        @Schema(description = "Assignee user ID") String assigneeId,
        @Schema(description = "Organisation ID") String organisationId,
        @Schema(description = "Status") Assignment.Status status,
        @Schema(description = "Report ID") String reportId,
        @Schema(description = "Report code") String reportCode,
        @Schema(description = "Report title") String reportTitle,
        @Schema(description = "Assigned at") Instant assignedAt,
        @Schema(description = "Completed at") Instant completedAt) {

    public static AssignmentDto from(Assignment assignment, Report report) {
        return new AssignmentDto(
                assignment.getId(),
                assignment.getAssignee() != null ? assignment.getAssignee().getId() : null,
                assignment.getOrganisation() != null ? assignment.getOrganisation().getId() : null,
                assignment.getStatus(),
                report.getId(),
                report.getCode(),
                report.getTitle(),
                assignment.getAssignedAt(),
                assignment.getCompletedAt()
        );
    }
}
