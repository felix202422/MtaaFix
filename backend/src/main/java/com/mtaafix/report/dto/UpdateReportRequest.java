package com.mtaafix.report.dto;

import com.mtaafix.report.domain.Report.Subject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update a report")
public record UpdateReportRequest(
        @Schema(description = "Report subject") Subject subject,
        @Schema(description = "Report title") @NotBlank String title,
        @Schema(description = "Description") String description,
        @Schema(description = "Location point") LocationDto location,
        @Schema(description = "Category id") String categoryId,
        @Schema(description = "Score") Integer score) {

    public static UpdateReportRequest of(Subject subject, String title, String description,
            LocationDto location, String categoryId, Integer score) {
        return new UpdateReportRequest(subject, title, description, location, categoryId, score);
    }
}
