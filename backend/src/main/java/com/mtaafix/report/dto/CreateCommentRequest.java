package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to add a comment to a report")
public record CreateCommentRequest(
        @Schema(description = "Comment content") @NotBlank String content) {
}
