package com.mtaafix.report.dto;

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
        @Schema(description = "Generalized latitude (2 decimals)") Double latitude,
        @Schema(description = "Generalized longitude (2 decimals)") Double longitude) {

    public static PublicReportSummaryDto from(Report r) {
        return new PublicReportSummaryDto(
                r.getCode(), r.getTitle(), r.getDescription(),
                r.getCategory() != null ? r.getCategory().getId() : null,
                r.getCategory() != null ? r.getCategory().getName() : null,
                r.getCategory() != null ? r.getCategory().getSlug() : null,
                r.getStatus(), r.getReportedAt(),
                PublicReportDetailDto.roundCoord(r.getLocation() != null ? r.getLocation().getY() : null),
                PublicReportDetailDto.roundCoord(r.getLocation() != null ? r.getLocation().getX() : null));
    }
}
