package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to add media evidence to a report")
public record CreateMediaRequest(
        @Schema(description = "Stored file name") @NotBlank String fileName,
        @Schema(description = "Original file name") @NotBlank String originalName,
        @Schema(description = "Content type") @NotBlank String contentType,
        @Schema(description = "File size in bytes") @NotNull Long sizeBytes,
        @Schema(description = "Download URL") @NotBlank String url,
        @Schema(description = "Media type") @NotBlank String mediaType) {
}
