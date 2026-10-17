package com.mtaafix.report.dto;

import com.mtaafix.user.domain.OrganisationMembership;
import com.mtaafix.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Organisation member (P21)")
public record MemberDto(
        @Schema(description = "User id") String userId,
        @Schema(description = "Email") String email,
        @Schema(description = "Full name") String fullName,
        @Schema(description = "Org role") String role,
        @Schema(description = "Account active") boolean active,
        @Schema(description = "Membership start") Instant joinedAt) {

    public static MemberDto from(User user, OrganisationMembership membership) {
        return new MemberDto(user.getId(), user.getEmail(), user.getFullName(),
                membership.getMemberRole().name(), user.isActive(), membership.getJoinedAt());
    }
}
