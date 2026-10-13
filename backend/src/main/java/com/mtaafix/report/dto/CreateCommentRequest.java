package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to add a comment to a report")
public record CreateCommentRequest(
        @Schema(description = "Comment content") String content) {
}
