package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to create a new report")
public record CreateReportRequest(
        @Schema(description = "Category id") @NotNull String categoryId,
        @Schema(description = "Report title") @NotBlank String title,
        @Schema(description = "Description") String description,
        @Schema(description = "Location point") @NotNull @Valid LocationDto location) {

    public static CreateReportRequest of(String categoryId, String title, String description,
            LocationDto location) {
        return new CreateReportRequest(categoryId, title, description, location);
    }
}
