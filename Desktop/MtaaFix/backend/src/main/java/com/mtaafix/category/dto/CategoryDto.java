package com.mtaafix.category.dto;

import com.mtaafix.report.domain.ReportCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Report category representation")
public record CategoryDto(
        @Schema(description = "Category ID") String id,
        @Schema(description = "Category name") String name,
        @Schema(description = "Category slug") String slug,
        @Schema(description = "Category type") ReportCategory.Type type) {

    public static CategoryDto from(ReportCategory category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getType()
        );
    }
}
