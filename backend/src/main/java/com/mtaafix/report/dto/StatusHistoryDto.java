package com.mtaafix.report.dto;

import com.mtaafix.report.domain.Report.Status;
import com.mtaafix.report.domain.ReportStatusHistory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Report status history entry")
public record StatusHistoryDto(
        @Schema(description = "Previous status") Status fromStatus,
        @Schema(description = "New status") Status toStatus,
        @Schema(description = "User id") String userId,
        @Schema(description = "Timestamp") String timestamp,
        @Schema(description = "Comment") String comment) {

    public static StatusHistoryDto from(ReportStatusHistory history) {
        return new StatusHistoryDto(history.getFromStatus(), history.getToStatus(),
                history.getUser() != null ? history.getUser().getId() : null,
                history.getCreatedAt() != null ? history.getCreatedAt().toString()
                        : null, history.getComment());
    }
}
