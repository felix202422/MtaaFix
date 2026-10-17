package com.mtaafix.user.domain;

import com.mtaafix.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Organisation membership: a user belongs to at most one organisation
 * (single-organisation model, Parts 3.5/3.6). Source of truth for org
 * scoping — replaces the legacy users.organisation_id column (V4, B-X.3).
 */
@Entity
@Table(name = "organization_members", indexes = {
        @Index(name = "uq_organization_members_user", columnList = "user_id", unique = true),
        @Index(name = "idx_organization_members_org", columnList = "organisation_id")
})
public class OrganisationMembership extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false, length = 36)
    private String userId;

    @Column(name = "organisation_id", nullable = false, updatable = false, length = 36)
    private String organisationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 30)
    private User.Role memberRole;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    public OrganisationMembership() {
    }

    public OrganisationMembership(String userId, String organisationId, User.Role memberRole) {
        this.userId = userId;
        this.organisationId = organisationId;
        this.memberRole = memberRole;
        this.joinedAt = Instant.now();
    }

    public String getUserId() {
        return userId;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public User.Role getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(User.Role memberRole) {
        this.memberRole = memberRole;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
