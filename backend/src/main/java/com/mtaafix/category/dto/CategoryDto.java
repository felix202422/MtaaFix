package com.mtaafix.category.dto;

import com.mtaafix.report.domain.ReportCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Report category representation (FR-5.1; active/displayOrder are "
        + "admin-managed and optional on write)")
public record CategoryDto(
        @Schema(description = "Category ID") String id,
        @Schema(description = "Category name") @NotBlank @Size(max = 100) String name,
        @Schema(description = "Category slug") @NotBlank @Size(max = 100) String slug,
        @Schema(description = "Category type") ReportCategory.Type type,
        @Schema(description = "Parent category id (hierarchy, FR-5.2)") String parentId,
        @Schema(description = "Whether the category can be chosen for new reports")
        Boolean active,
        @Schema(description = "Display order") Integer displayOrder) {

    public static CategoryDto from(ReportCategory category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getType(),
                category.getParentId(),
                category.isActive(),
                category.getDisplayOrder()
        );
    }
}
