package com.mtaafix.user.service;

import com.mtaafix.audit.service.AuditService;
import com.mtaafix.common.exceptions.AccountInactiveException;
import com.mtaafix.common.exceptions.InvalidCredentialsException;
import com.mtaafix.common.exceptions.InvalidTokenException;
import com.mtaafix.common.exceptions.ResourceNotFoundException;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.domain.User.Role;
import com.mtaafix.user.dto.AuthResponse;
import com.mtaafix.user.dto.RegisterRequest;
import com.mtaafix.user.dto.SetActiveRequest;
import com.mtaafix.user.dto.UpdateProfileRequest;
import com.mtaafix.user.dto.UpdateRoleRequest;
import com.mtaafix.user.dto.UserDto;
import com.mtaafix.user.repository.UserRepository;
import com.mtaafix.security.JwtTokenProvider;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final OrganisationMembershipService membershipService;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, JwtTokenProvider tokenProvider,
            OrganisationMembershipService membershipService, AuditService auditService) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.membershipService = membershipService;
        this.auditService = auditService;
    }

    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        String passwordHash = BCrypt.hashpw(request.password(), BCrypt.gensalt());
        User user = new User(request.email(), passwordHash, request.fullName(),
                request.mobile(), Role.CITIZEN);
        user = userRepository.save(user);
        return toDto(user);
    }

    public AuthResponse authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!user.isActive()) {
            throw new AccountInactiveException();
        }
        String accessToken =
                tokenProvider.generateAccessToken(user.getId(), user.getRole().name(), user.getTokenVersion());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getTokenVersion());
        return AuthResponse.of(accessToken, refreshToken, 900L, toDto(user));
    }

    public User refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()
                || !tokenProvider.validateToken(refreshToken)
                || !JwtTokenProvider.TYPE_REFRESH.equals(tokenProvider.getType(refreshToken))) {
            throw new InvalidTokenException();
        }
        String subject = tokenProvider.getSubject(refreshToken);
        User user = userRepository.findById(subject)
                .orElseThrow(() -> new InvalidTokenException("Token subject does not exist."));
        if (!user.isActive()) {
            throw new AccountInactiveException();
        }
        if (tokenProvider.getTokenVersion(refreshToken) != user.getTokenVersion()) {
            throw new InvalidTokenException("Token has been revoked.");
        }
        return user;
    }

    public UserDto findById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return toDto(user);
    }

    public UserDto findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return toDto(user);
    }

    public UserDto updateProfile(User user, UpdateProfileRequest request) {
        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new IllegalArgumentException("Full name must not be blank.");
        }
        user.setFullName(request.fullName().trim());
        if (request.mobile() != null) {
            user.setMobile(request.mobile().isBlank() ? null : request.mobile().trim());
        }
        return toDto(userRepository.save(user));
    }

    public UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getMobile(),
                user.isActive(), user.getRole(), membershipService.orgIdOfOrNull(user));
    }

    /** FR-12.1: admin user search (email/full-name text, role, active filters). */
    public Page<UserDto> search(String q, String role, Boolean active, Pageable pageable) {
        String like = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        Role roleEnum = null;
        if (role != null && !role.isBlank()) {
            try {
                roleEnum = Role.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unknown role filter: " + role);
            }
        }
        return userRepository.search(like, roleEnum, active, pageable).map(this::toDto);
    }

    /** FR-5.4/FR-12.1/FR-12.4: role grant, ADMINISTRATOR only, audited. */
    public UserDto changeRole(String id, UpdateRoleRequest request, User actor) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (target.getId().equals(actor.getId())) {
            throw new IllegalArgumentException("You cannot change your own role.");
        }
        Role newRole;
        try {
            newRole = Role.valueOf(request.role().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown role: " + request.role());
        }
        Role oldRole = target.getRole();
        if (oldRole == newRole) {
            return toDto(target);
        }
        if (oldRole == Role.ADMINISTRATOR
                && userRepository.countByRoleAndActiveTrue(Role.ADMINISTRATOR) <= 1) {
            throw new IllegalStateException("Cannot demote the last active administrator.");
        }
        boolean hasMembership = membershipService.orgIdOfUserId(target.getId()) != null;
        boolean orgRole = newRole == Role.ORGANIZATION_ADMIN || newRole == Role.FIELD_WORKER;
        if (orgRole && !hasMembership) {
            throw new IllegalArgumentException(
                    "Organisation roles must be granted through an organisation membership "
                            + "(add the user to an organisation first).");
        }
        if (!orgRole && hasMembership) {
            membershipService.remove(target.getId());
        }
        target.setRole(newRole);
        User saved = userRepository.save(target);
        Map<String, String> details = new HashMap<>();
        details.put("email", target.getEmail());
        details.put("from", oldRole.name());
        details.put("to", newRole.name());
        if (!orgRole && hasMembership) {
            details.put("membershipRemoved", "true");
        }
        auditService.record("USER_ROLE_CHANGED", actor, "User", target.getId(), details);
        return toDto(saved);
    }

    /** FR-12.1: activate/deactivate, ADMINISTRATOR only, audited. */
    public UserDto setActive(String id, SetActiveRequest request, User actor) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (target.getId().equals(actor.getId())) {
            throw new IllegalArgumentException("You cannot change your own account status.");
        }
        boolean active = request.active();
        if (!active && target.isActive() && target.getRole() == Role.ADMINISTRATOR
                && userRepository.countByRoleAndActiveTrue(Role.ADMINISTRATOR) <= 1) {
            throw new IllegalStateException("Cannot deactivate the last active administrator.");
        }
        target.setActive(active);
        User saved = userRepository.save(target);
        Map<String, String> details = new HashMap<>();
        details.put("email", target.getEmail());
        auditService.record(active ? "USER_ACTIVATED" : "USER_DEACTIVATED", actor,
                "User", target.getId(), details);
        return toDto(saved);
    }
}
