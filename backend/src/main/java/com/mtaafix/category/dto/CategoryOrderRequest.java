package com.mtaafix.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to set a category's display order (FR-5.1)")
public record CategoryOrderRequest(
        @Schema(description = "New display order (lower sorts first)") Integer displayOrder) {
}
