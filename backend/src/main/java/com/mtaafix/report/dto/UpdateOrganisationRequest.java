package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to update an organisation (FR-5.3; null fields keep current "
        + "values; slug is immutable after creation)")
public record UpdateOrganisationRequest(
        @Schema(description = "Name") @Size(max = 255) String name,
        @Schema(description = "Contact email") @Email @Size(max = 255) String email,
        @Schema(description = "Contact phone") @Size(max = 50) String phone,
        @Schema(description = "Address") @Size(max = 1000) String address,
        @Schema(description = "Description") @Size(max = 2000) String description,
        @Schema(description = "Logo URL") @Size(max = 500) String logoUrl) {
}
