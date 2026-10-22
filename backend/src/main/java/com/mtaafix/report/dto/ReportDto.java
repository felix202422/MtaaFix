package com.mtaafix.report.dto;

import com.mtaafix.common.geo.GeoJsonPoint;
import com.mtaafix.report.domain.Report;
import com.mtaafix.report.domain.Report.Status;
import com.mtaafix.report.domain.ReportMedia;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "Report representation")
public record ReportDto(
        @Schema(description = "Report id") String id,
        @Schema(description = "Report code") String code,
        @Schema(description = "Title") String title,
        @Schema(description = "Description") String description,
        @Schema(description = "Location (exact; authorized surfaces only)") LocationDto location,
        @Schema(description = "GeoJSON Point (exact, FR-11.5; authorized surfaces only)") GeoJsonPoint geoJson,
        @Schema(description = "Reported at") Instant reportedAt,
        @Schema(description = "Status") Status status,
        @Schema(description = "Score") Integer score,
        @Schema(description = "Category id") String categoryId,
        @Schema(description = "Assignment id") String assignmentId,
        @Schema(description = "Organisation id") String organisationId,
        @Schema(description = "Current assignee id") String assigneeId,
        @Schema(description = "Duplicate of report id") String duplicateOfId,
        @Schema(description = "Status history") List<StatusHistoryDto> statusHistory,
        @Schema(description = "Media") List<ReportMediaDto> media,
        @Schema(description = "Comments") List<ReportCommentDto> comments) {

    public static ReportDto from(Report r) {
        LocationDto location = locationOf(r);
        return new ReportDto(
                r.getId(), r.getCode(), r.getTitle(), r.getDescription(),
                location,
                location != null ? GeoJsonPoint.of(location.latitude(), location.longitude()) : null,
                r.getReportedAt(), r.getStatus(), r.getScore(),
                r.getCategory() != null ? r.getCategory().getId() : null,
                r.getAssignment() != null ? r.getAssignment().getId() : null,
                r.getOrganisation() != null ? r.getOrganisation().getId() : null,
                r.getAssignment() != null ? r.getAssignment().getAssignee() != null
                        ? r.getAssignment().getAssignee().getId() : null
                        : null,
                r.getDuplicateOf() != null ? r.getDuplicateOf().getId() : null,
                r.getStatusHistory().stream()
                        .map(StatusHistoryDto::from)
                        .collect(Collectors.toList()),
                r.getMedia().stream()
                        .map(ReportMediaDto::from)
                        .collect(Collectors.toList()),
                r.getComments().stream()
                        .map(ReportCommentDto::from)
                        .collect(Collectors.toList()));
    }

    /**
     * Phase 17 list-row mapping (NFR-7): same scalar contract as {@link #from}
     * but never touches the statusHistory/comments lazy collections and takes
     * the media collection pre-fetched by the caller (one batch query per
     * page). No list UI reads statusHistory or comments (Phase 17 audit);
     * media is included because list UIs render photo counts from it.
     * Detail endpoints keep the full {@link #from} mapping.
     */
    public static ReportDto listItem(Report r, List<ReportMedia> media) {
        LocationDto location = locationOf(r);
        return new ReportDto(
                r.getId(), r.getCode(), r.getTitle(), r.getDescription(),
                location,
                location != null ? GeoJsonPoint.of(location.latitude(), location.longitude()) : null,
                r.getReportedAt(), r.getStatus(), r.getScore(),
                r.getCategory() != null ? r.getCategory().getId() : null,
                r.getAssignment() != null ? r.getAssignment().getId() : null,
                r.getOrganisation() != null ? r.getOrganisation().getId() : null,
                r.getAssignment() != null ? r.getAssignment().getAssignee() != null
                        ? r.getAssignment().getAssignee().getId() : null
                        : null,
                r.getDuplicateOf() != null ? r.getDuplicateOf().getId() : null,
                List.of(),
                media.stream()
                        .map(ReportMediaDto::from)
                        .collect(Collectors.toList()),
                List.of());
    }

    private static LocationDto locationOf(Report r) {
        return r.getLocation() != null
                ? new LocationDto(r.getLocation().getY(), r.getLocation().getX(), null)
                : null;
    }

    public static ReportDto of(Report report) {
        return from(report);
    }
}
