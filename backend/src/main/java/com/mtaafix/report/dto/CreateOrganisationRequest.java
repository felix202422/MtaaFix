package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create an organisation (FR-5.3)")
public record CreateOrganisationRequest(
        @Schema(description = "Name") @NotBlank @Size(max = 255) String name,
        @Schema(description = "Slug (lowercase letters, digits, dashes)")
        @NotBlank @Size(max = 100) @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "Slug must contain lowercase letters, digits and single dashes only")
        String slug,
        @Schema(description = "Contact email") @Email @Size(max = 255) String email,
        @Schema(description = "Contact phone") @Size(max = 50) String phone,
        @Schema(description = "Address") @Size(max = 1000) String address,
        @Schema(description = "Description") @Size(max = 2000) String description) {
}
