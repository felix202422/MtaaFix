package com.mtaafix.report.dto;

import com.mtaafix.report.domain.ReportMedia;
import com.mtaafix.report.domain.ReportMedia.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Report media attachment")
public record ReportMediaDto(
        @Schema(description = "Media id") String id,
        @Schema(description = "Stored file name") String fileName,
        @Schema(description = "Original file name") String originalName,
        @Schema(description = "Content type") String contentType,
        @Schema(description = "File size in bytes") Long sizeBytes,
        @Schema(description = "Download url") String url,
        @Schema(description = "Media type") MediaType mediaType) {

    public static ReportMediaDto from(ReportMedia media) {
        return new ReportMediaDto(media.getId(), media.getFileName(), media.getOriginalName(),
                media.getContentType(), media.getSizeBytes(), media.getUrl(), media.getMediaType());
    }
}
