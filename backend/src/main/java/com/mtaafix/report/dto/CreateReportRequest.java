package com.mtaafix.report.dto;

import com.mtaafix.report.domain.Report.Subject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to create a new report")
public record CreateReportRequest(
        @Schema(description = "Report subject") @NotNull Subject subject,
        @Schema(description = "Report title") @NotBlank String title,
        @Schema(description = "Description") String description,
        @Schema(description = "Location point") @NotNull LocationDto location,
        @Schema(description = "Category id") String categoryId) {

    public static CreateReportRequest of(Subject subject, String title, String description,
            LocationDto location, String categoryId) {
        return new CreateReportRequest(subject, title, description, location, categoryId);
    }
}
