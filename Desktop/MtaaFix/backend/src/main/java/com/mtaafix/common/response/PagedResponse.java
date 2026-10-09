package com.mtaafix.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Generic paginated response")
public record PagedResponse<T>(
        @Schema(description = "Total number of elements") int totalElements,
        @Schema(description = "Total pages") int totalPages,
        @Schema(description = "Current page number (0-indexed)") int page,
        @Schema(description = "Number of elements per page") long size,
        @Schema(description = "List of items") List<T> content) {

    public static <T> PagedResponse<T> of(int totalElements, int totalPages, int page, long size,
            List<T> content) {
        return new PagedResponse<>(totalElements, totalPages, page, size, content);
    }
}
