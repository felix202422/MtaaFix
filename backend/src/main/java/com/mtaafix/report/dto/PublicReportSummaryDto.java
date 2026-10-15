package com.mtaafix.report.dto;

import com.mtaafix.common.geo.GeoJsonPoint;
import com.mtaafix.common.geo.LocationGeneralizer;
import com.mtaafix.report.domain.Report;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Public report summary (FR-8.3 explore feed; public fields only)")
public record PublicReportSummaryDto(
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
        @Schema(description = "Generalized GeoJSON point (fuzzed, FR-11.5)") GeoJsonPoint geoJson) {

    public static PublicReportSummaryDto from(Report r, LocationGeneralizer generalizer) {
        LocationGeneralizer.GeneralizedLocation geo =
                generalizer.generalize(r.getId(), r.getLocation());
        return new PublicReportSummaryDto(
                r.getCode(), r.getTitle(), r.getDescription(),
                r.getCategory() != null ? r.getCategory().getId() : null,
                r.getCategory() != null ? r.getCategory().getName() : null,
                r.getCategory() != null ? r.getCategory().getSlug() : null,
                r.getStatus(), r.getReportedAt(),
                geo != null ? geo.latitude() : null,
                geo != null ? geo.longitude() : null,
                geo != null ? geo.geoJson() : null);
    }
}
