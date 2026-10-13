package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to add media evidence to a report")
public record CreateMediaRequest(
        @Schema(description = "Stored file name") String fileName,
        @Schema(description = "Original file name") String originalName,
        @Schema(description = "Content type") String contentType,
        @Schema(description = "File size in bytes") Long sizeBytes,
        @Schema(description = "Download URL") String url,
        @Schema(description = "Media type") String mediaType) {
}
