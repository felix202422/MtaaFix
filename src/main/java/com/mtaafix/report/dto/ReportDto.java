package com.mtaafix.report.dto;

import com.mtaafix.report.domain.Report;
import com.mtaafix.report.domain.Report.Status;
import com.mtaafix.report.domain.Report.Subject;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "Report representation")
public record ReportDto(
        @Schema(description = "Report id") String id,
        @Schema(description = "Report code") String code,
        @Schema(description = "Report subject") Subject subject,
        @Schema(description = "Title") String title,
        @Schema(description = "Description") String description,
        @Schema(description = "Location") LocationDto location,
        @Schema(description = "Reported at") Instant reportedAt,
        @Schema(description = "Status") Status status,
        @Schema(description = "Score") Integer score,
        @Schema(description = "Category id") String categoryId,
        @Schema(description = "Assignment id") String assignmentId,
        @Schema(description = "Organisation id") String organisationId,
        @Schema(description = "Current assignee id") String assigneeId,
        @Schema(description = "Status history") List<StatusHistoryDto> statusHistory,
        @Schema(description = "Media") List<ReportMediaDto> media,
        @Schema(description = "Comments") List<ReportCommentDto> comments) {

    public static ReportDto from(Report report) {
        return new ReportDto(
                report.getId(),
                report.getCode(),
                report.getSubject(),
                report.getTitle(),
                report.getDescription(),
                report.getLocation() != null ? new LocationDto(
                        report.getLocation().getY(), report.getLocation().getX(), null) : null,
                report.getReportedAt(),
                report.getStatus(),
                report.getScore(),
                report.getCategory() != null ? report.getCategory().getId() : null,
                report.getAssignment() != null ? report.getAssignment().getId() : null,
                report.getOrganisation() != null ? report.getOrganisation().getId() : null,
                report.getAssignment() != null ? report.getAssignment().getAssignee() != null
                        ? report.getAssignment().getAssignee().getId() : null
                        : null,
                report.getStatusHistory().stream()
                        .map(StatusHistoryDto::from)
                        .collect(Collectors.toList()),
                report.getMedia().stream()
                        .map(ReportMediaDto::from)
                        .collect(Collectors.toList()),
                report.getComments().stream()
                        .map(ReportCommentDto::from)
                        .collect(Collectors.toList()));
    }

    public static ReportDto of(Report report) {
        return from(report);
    }
}
