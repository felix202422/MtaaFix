package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to update a report")
public record UpdateReportRequest(
        @Schema(description = "Report title") @NotBlank String title,
        @Schema(description = "Description") String description,
        @Schema(description = "Location point") LocationDto location,
        @Schema(description = "Category id") String categoryId,
        @Schema(description = "Score") Integer score) {

    public static UpdateReportRequest of(String title, String description,
            LocationDto location, String categoryId, Integer score) {
        return new UpdateReportRequest(title, description, location, categoryId, score);
    }
}
