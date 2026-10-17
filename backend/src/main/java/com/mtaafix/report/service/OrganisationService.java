package com.mtaafix.report.service;

import com.mtaafix.audit.service.AuditService;
import com.mtaafix.common.exceptions.ResourceNotFoundException;
import com.mtaafix.report.domain.Organisation;
import com.mtaafix.report.dto.AddMemberRequest;
import com.mtaafix.report.dto.CreateOrganisationRequest;
import com.mtaafix.report.dto.MemberDto;
import com.mtaafix.report.dto.OrganisationDto;
import com.mtaafix.report.dto.UpdateOrganisationRequest;
import com.mtaafix.report.repository.OrganisationRepository;
import com.mtaafix.user.domain.OrganisationMembership;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
import com.mtaafix.user.service.OrganisationMembershipService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-5.3 organisation management + P21 team membership (B-X.3).
 * ADMINISTRATOR (A-scope, audited) or ORGANIZATION_ADMIN limited to own org
 * for reads and member management (matrix areas 8/9).
 */
@Service
public class OrganisationService {

    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final OrganisationMembershipService membershipService;
    private final AuditService auditService;

    public OrganisationService(OrganisationRepository organisationRepository,
            UserRepository userRepository, OrganisationMembershipService membershipService,
            AuditService auditService) {
        this.organisationRepository = organisationRepository;
        this.userRepository = userRepository;
        this.membershipService = membershipService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<OrganisationDto> list(User actor) {
        if (actor.getRole() == User.Role.ADMINISTRATOR) {
            return organisationRepository.findAll().stream()
                    .map(org -> OrganisationDto.from(org,
                            membershipService.countMembers(org.getId())))
                    .toList();
        }
        if (actor.getRole() == User.Role.ORGANIZATION_ADMIN) {
            String ownOrgId = membershipService.orgIdOfOrNull(actor);
            if (ownOrgId == null) {
                throw new AccessDeniedException("You are not linked to an organisation.");
            }
            return organisationRepository.findById(ownOrgId)
                    .map(org -> List.of(OrganisationDto.from(org,
                            membershipService.countMembers(org.getId()))))
                    .orElse(List.of());
        }
        throw new AccessDeniedException("Organisation management is not available for your role.");
    }

    @Transactional(readOnly = true)
    public OrganisationDto get(String id, User actor) {
        Organisation org = loadChecked(id, actor);
        return OrganisationDto.from(org, membershipService.countMembers(id));
    }

    @Transactional
    public OrganisationDto create(CreateOrganisationRequest request, User actor) {
        String slug = request.slug().trim();
        if (organisationRepository.findBySlug(slug).isPresent()) {
            throw new IllegalStateException("Organisation slug already exists: " + slug);
        }
        Organisation org = new Organisation(request.name().trim(), slug);
        org.setEmail(trimToNull(request.email()));
        org.setPhone(trimToNull(request.phone()));
        org.setAddress(trimToNull(request.address()));
        org.setDescription(trimToNull(request.description()));
        org = organisationRepository.save(org);
        auditService.record("ORGANIZATION_CREATED", actor, "Organisation", org.getId(),
                Map.of("slug", org.getSlug(), "name", org.getName()));
        return OrganisationDto.from(org, 0);
    }

    @Transactional
    public OrganisationDto update(String id, UpdateOrganisationRequest request, User actor) {
        Organisation org = organisationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation", id));
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new IllegalArgumentException("Name must not be blank.");
            }
            org.setName(request.name().trim());
        }
        if (request.email() != null) {
            org.setEmail(trimToNull(request.email()));
        }
        if (request.phone() != null) {
            org.setPhone(trimToNull(request.phone()));
        }
        if (request.address() != null) {
            org.setAddress(trimToNull(request.address()));
        }
        if (request.description() != null) {
            org.setDescription(trimToNull(request.description()));
        }
        if (request.logoUrl() != null) {
            org.setLogoUrl(trimToNull(request.logoUrl()));
        }
        org = organisationRepository.save(org);
        auditService.record("ORGANIZATION_UPDATED", actor, "Organisation", org.getId(),
                Map.of("slug", org.getSlug()));
        return OrganisationDto.from(org, membershipService.countMembers(id));
    }

    @Transactional
    public OrganisationDto setActive(String id, boolean active, User actor) {
        Organisation org = organisationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation", id));
        org.setActive(active);
        org = organisationRepository.save(org);
        auditService.record(active ? "ORGANIZATION_ACTIVATED" : "ORGANIZATION_DEACTIVATED",
                actor, "Organisation", org.getId(), Map.of("slug", org.getSlug()));
        return OrganisationDto.from(org, membershipService.countMembers(id));
    }

    @Transactional(readOnly = true)
    public List<MemberDto> listMembers(String id, User actor) {
        Organisation org = loadChecked(id, actor);
        List<OrganisationMembership> members = membershipService.membersOf(org.getId());
        return members.stream()
                .map(m -> userRepository.findById(m.getUserId())
                        .map(u -> MemberDto.from(u, m))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /** Add member: validates scope, grants the org role (FR-5.4), creates membership. */
    @Transactional
    public MemberDto addMember(String id, AddMemberRequest request, User actor) {
        Organisation org = loadChecked(id, actor);
        User target = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));
        User.Role memberRole = User.Role.valueOf(request.role());
        User.Role previousRole = target.getRole();
        if (previousRole != memberRole) {
            target.setRole(memberRole);
            userRepository.save(target);
            Map<String, String> roleDetails = new HashMap<>();
            roleDetails.put("email", target.getEmail());
            roleDetails.put("from", previousRole.name());
            roleDetails.put("to", memberRole.name());
            roleDetails.put("organisationId", org.getId());
            auditService.record("USER_ROLE_CHANGED", actor, "User", target.getId(), roleDetails);
        }
        OrganisationMembership membership = membershipService.add(target.getId(), org.getId(),
                memberRole);
        auditService.record("MEMBERSHIP_ADDED", actor, "Organisation", org.getId(),
                Map.of("userId", target.getId(), "role", memberRole.name()));
        return MemberDto.from(target, membership);
    }

    @Transactional
    public void removeMember(String id, String userId, User actor) {
        Organisation org = loadChecked(id, actor);
        if (!membershipService.isMember(userId, org.getId())) {
            throw new ResourceNotFoundException("Membership", userId);
        }
        membershipService.remove(userId);
        auditService.record("MEMBERSHIP_REMOVED", actor, "Organisation", org.getId(),
                Map.of("userId", userId));
    }

    private Organisation loadChecked(String id, User actor) {
        Organisation org = organisationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation", id));
        if (actor.getRole() == User.Role.ADMINISTRATOR) {
            return org;
        }
        if (actor.getRole() == User.Role.ORGANIZATION_ADMIN) {
            String ownOrgId = membershipService.orgIdOfOrNull(actor);
            if (ownOrgId != null && ownOrgId.equals(id)) {
                return org;
            }
            throw new AccessDeniedException("Organisation is outside your organisation scope.");
        }
        throw new AccessDeniedException("Organisation management is not available for your role.");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
