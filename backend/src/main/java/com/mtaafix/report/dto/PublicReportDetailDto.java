package com.mtaafix.report.dto;

import com.mtaafix.common.geo.GeoJsonPoint;
import com.mtaafix.common.geo.LocationGeneralizer;
import com.mtaafix.report.domain.Report;
import com.mtaafix.report.domain.ReportStatusHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Schema(description = "Public report detail (FR-8.4; public fields only, generalized location)")
public record PublicReportDetailDto(
        @Schema(description = "Public reference code") String code,
        @Schema(description = "Title") String title,
        @Schema(description = "Description") String description,
        @Schema(description = "Category id") String categoryId,
        @Schema(description = "Category name") String categoryName,
        @Schema(description = "Category slug") String categorySlug,
        @Schema(description = "Status") Report.Status status,
        @Schema(description = "Submission date") Instant reportedAt,
        @Schema(description = "Generalized latitude (distance fuzzing, FR-11.4)") Double latitude,
        @Schema(description = "Generalized longitude (distance fuzzing, FR-11.4)") Double longitude,
        @Schema(description = "Generalized GeoJSON point (fuzzed, FR-11.5)") GeoJsonPoint geoJson,
        @Schema(description = "Resolved timestamp (null until resolved)") Instant resolvedAt,
        @Schema(description = "Closed timestamp (null until closed)") Instant closedAt,
        @Schema(description = "Canonical reference when this report is a duplicate") String duplicateOfCode,
        @Schema(description = "High-level public status timeline") List<PublicTimelineEntryDto> timeline) {

    public static PublicReportDetailDto from(Report r, LocationGeneralizer generalizer) {
        List<ReportStatusHistory> history = r.getStatusHistory().stream()
                .filter(PublicReportDetailDto::isPublicTransition)
                .sorted(Comparator.comparing(ReportStatusHistory::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        Instant resolvedAt = firstAt(history, Report.Status.RESOLVED);
        Instant closedAt = firstAt(history, Report.Status.CLOSED);
        LocationGeneralizer.GeneralizedLocation geo =
                generalizer.generalize(r.getId(), r.getLocation());
        return new PublicReportDetailDto(
                r.getCode(), r.getTitle(), r.getDescription(),
                r.getCategory() != null ? r.getCategory().getId() : null,
                r.getCategory() != null ? r.getCategory().getName() : null,
                r.getCategory() != null ? r.getCategory().getSlug() : null,
                r.getStatus(), r.getReportedAt(),
                geo != null ? geo.latitude() : null,
                geo != null ? geo.longitude() : null,
                geo != null ? geo.geoJson() : null,
                resolvedAt, closedAt,
                r.getDuplicateOf() != null ? r.getDuplicateOf().getCode() : null,
                history.stream()
                        .map(h -> new PublicTimelineEntryDto(h.getToStatus(), h.getCreatedAt()))
                        .toList());
    }

    static boolean isPublicTransition(ReportStatusHistory h) {
        Report.Status from = h.getFromStatus();
        Report.Status to = h.getToStatus();
        if (from == Report.Status.SUBMITTED && to == Report.Status.REJECTED) {
            return false;
        }
        if (from == Report.Status.ASSIGNED && to == Report.Status.ASSIGNED) {
            return false;
        }
        return true;
    }

    private static Instant firstAt(List<ReportStatusHistory> history, Report.Status status) {
        return history.stream()
                .filter(h -> h.getToStatus() == status)
                .map(ReportStatusHistory::getCreatedAt)
                .filter(t -> t != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }
}
