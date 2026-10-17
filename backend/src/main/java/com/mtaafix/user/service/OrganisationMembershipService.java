package com.mtaafix.user.service;

import com.mtaafix.user.domain.OrganisationMembership;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.OrganisationMembershipRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Organisation membership reads/writes. Org-scoped authorisation across
 * modules resolves the caller's organisation through this service
 * (single source of truth since V4).
 */
@Service
public class OrganisationMembershipService {

    private final OrganisationMembershipRepository membershipRepository;

    public OrganisationMembershipService(OrganisationMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    /** The user's organisation id, or null when not a member of any. */
    @Transactional(readOnly = true)
    public String orgIdOfOrNull(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        return orgIdOfUserId(user.getId());
    }

    /** The organisation id for a user id, or null when not a member. */
    @Transactional(readOnly = true)
    public String orgIdOfUserId(String userId) {
        if (userId == null) {
            return null;
        }
        return membershipRepository.findByUserId(userId)
                .map(OrganisationMembership::getOrganisationId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean isMember(String userId, String organisationId) {
        return membershipRepository.findByUserIdAndOrganisationId(userId, organisationId).isPresent();
    }

    @Transactional(readOnly = true)
    public List<OrganisationMembership> membersOf(String organisationId) {
        return membershipRepository.findByOrganisationIdOrderByJoinedAtAsc(organisationId);
    }

    @Transactional(readOnly = true)
    public long countMembers(String organisationId) {
        return membershipRepository.countByOrganisationId(organisationId);
    }

    /**
     * Add a member (single-organisation model: rejects a second affiliation).
     * The caller is responsible for the matching role grant and audit event.
     */
    @Transactional
    public OrganisationMembership add(String userId, String organisationId, User.Role memberRole) {
        if (membershipRepository.findByUserId(userId).isPresent()) {
            throw new IllegalStateException("User already belongs to an organisation");
        }
        return membershipRepository.save(new OrganisationMembership(userId, organisationId, memberRole));
    }

    @Transactional
    public void remove(String userId) {
        membershipRepository.deleteByUserId(userId);
    }
}
