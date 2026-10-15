package com.mtaafix.report.dto;

import com.mtaafix.report.domain.Report;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "High-level public timeline entry (status + timestamp only)")
public record PublicTimelineEntryDto(
        @Schema(description = "Status reached") Report.Status status,
        @Schema(description = "Timestamp") Instant timestamp) {
}
