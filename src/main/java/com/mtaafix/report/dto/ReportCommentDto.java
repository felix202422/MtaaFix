package com.mtaafix.report.dto;

import com.mtaafix.report.domain.ReportComment;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Report comment")
public record ReportCommentDto(
        @Schema(description = "Comment id") String id,
        @Schema(description = "Content") String content,
        @Schema(description = "Commenting user id") String userId,
        @Schema(description = "Timestamp") String timestamp) {

    public static ReportCommentDto from(ReportComment comment) {
        return new ReportCommentDto(comment.getId(), comment.getContent(), comment.getUser() != null
                ? comment.getUser().getId() : null,
                comment.getCreatedAt() != null ? comment.getCreatedAt().toString() : null);
    }
}
