package com.mtaafix.report.dto;

import com.mtaafix.report.domain.Organisation;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Organisation (FR-5.3, P21)")
public record OrganisationDto(
        @Schema(description = "Organisation id") String id,
        @Schema(description = "Name") String name,
        @Schema(description = "Slug") String slug,
        @Schema(description = "Contact email") String email,
        @Schema(description = "Contact phone") String phone,
        @Schema(description = "Address") String address,
        @Schema(description = "Description") String description,
        @Schema(description = "Logo URL") String logoUrl,
        @Schema(description = "Account status") boolean active,
        @Schema(description = "Number of members") long memberCount) {

    public static OrganisationDto from(Organisation org, long memberCount) {
        return new OrganisationDto(org.getId(), org.getName(), org.getSlug(), org.getEmail(),
                org.getPhone(), org.getAddress(), org.getDescription(), org.getLogoUrl(),
                org.isActive(), memberCount);
    }
}
