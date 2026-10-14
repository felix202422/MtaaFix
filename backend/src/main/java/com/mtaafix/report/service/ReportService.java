package com.mtaafix.report.service;

import com.mtaafix.audit.service.AuditService;
import com.mtaafix.common.exceptions.ResourceNotFoundException;
import com.mtaafix.common.response.PagedResponse;
import com.mtaafix.notification.domain.Notification.Type;
import com.mtaafix.notification.service.NotificationService;
import com.mtaafix.report.domain.Assignment;
import com.mtaafix.report.domain.Organisation;
import com.mtaafix.report.domain.Report;
import com.mtaafix.report.domain.Report.Status;
import com.mtaafix.report.domain.ReportCategory;
import com.mtaafix.report.domain.ReportComment;
import com.mtaafix.report.domain.ReportMedia;
import com.mtaafix.report.dto.ConfirmReportRequest;
import com.mtaafix.report.dto.CompleteReportRequest;
import com.mtaafix.report.dto.CreateCommentRequest;
import com.mtaafix.report.dto.CreateReportRequest;
import com.mtaafix.report.dto.DisputeReportRequest;
import com.mtaafix.report.dto.DuplicateReportRequest;
import com.mtaafix.report.dto.NeedsInfoRequest;
import com.mtaafix.report.dto.RejectReportRequest;
import com.mtaafix.report.dto.ReportDto;
import com.mtaafix.report.dto.ReopenReportRequest;
import com.mtaafix.report.dto.ReturnReportRequest;
import com.mtaafix.report.dto.UpdateReportRequest;
import com.mtaafix.report.lifecycle.LifecycleAction;
import com.mtaafix.report.lifecycle.ReportLifecycle;
import com.mtaafix.report.mapper.ReportMapper;
import com.mtaafix.report.repository.AssignmentRepository;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.domain.User.Role;
import com.mtaafix.user.repository.UserRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private static final Set<String> ALLOWED_MEDIA_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_MEDIA_BYTES = 20L * 1024 * 1024;

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AssignmentRepository assignmentRepository;
    private final ReportLifecycle lifecycle;
    private final ReportCodeGenerator codeGenerator;
    private final AuditService auditService;

    public ReportService(ReportRepository reportRepository, UserRepository userRepository,
            NotificationService notificationService, AssignmentRepository assignmentRepository,
            ReportLifecycle lifecycle, ReportCodeGenerator codeGenerator,
            AuditService auditService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.assignmentRepository = assignmentRepository;
        this.lifecycle = lifecycle;
        this.codeGenerator = codeGenerator;
        this.auditService = auditService;
    }

    @Transactional
    public ReportDto create(CreateReportRequest request, User creator) {
        ReportCategory category = reportRepository.findCategoryById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
        Report report = ReportMapper.toEntity(request, category, creator);
        report.setCode(codeGenerator.next());
        report = reportRepository.save(report);
        notifyUser(creator, Type.REPORT_SUBMITTED, report,
                "Report submitted", "Your report " + report.getCode() + " was submitted.");
        return ReportMapper.toDto(report);
    }

    @Transactional(readOnly = true)
    public ReportDto get(String id, User user) {
        Report report = load(id);
        assertCanRead(report, user);
        return ReportMapper.toDto(report);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReportDto> list(String status, String categoryId, String orgId, User user,
            Pageable pageable) {
        Status statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = Status.valueOf(status);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unknown status filter: " + status);
            }
        }
        String scopedOrgId = orgId;
        String userId = null;
        String assigneeId = null;
        switch (user.getRole()) {
            case CITIZEN -> userId = user.getId();
            case FIELD_WORKER -> assigneeId = user.getId();
            case ORGANIZATION_ADMIN -> {
                if (user.getOrganisationId() != null) {
                    scopedOrgId = user.getOrganisationId();
                } else {
                    userId = user.getId();
                }
            }
            case MODERATOR, ADMINISTRATOR -> { }
        }
        Page<Report> page = reportRepository.findByScope(statusEnum, categoryId, scopedOrgId, userId,
                assigneeId, pageable);
        List<ReportDto> content = page.getContent().stream()
                .map(ReportMapper::toDto)
                .toList();
        return PagedResponse.of((int) page.getTotalElements(), page.getTotalPages(), page.getNumber(),
                (int) page.getSize(), content);
    }

    @Transactional
    public ReportDto submit(String id, User user) {
        Report report = load(id);
        lifecycle.apply(report, LifecycleAction.CLAIM_REVIEW, user, null, null);
        report = reportRepository.save(report);
        notifyUser(report.getUser(), Type.REPORT_SUBMITTED, report,
                "Your report is under review", "Moderator review is in progress.");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto needsInfo(String id, User user, NeedsInfoRequest request) {
        Report report = load(id);
        lifecycle.apply(report, LifecycleAction.REQUEST_INFO, user, request.message(), null);
        report = reportRepository.save(report);
        auditReport("REPORT_NEEDS_INFO", user, report);
        notifyUser(report.getUser(), Type.REPORT_NEEDS_INFO, report,
                "More information needed", request.message());
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto resubmit(String id, User user) {
        Report report = load(id);
        assertCreator(report, user, "Only the report creator can resubmit this report.");
        lifecycle.apply(report, LifecycleAction.RESUBMIT, user, null, null);
        report = reportRepository.save(report);
        for (User moderator : userRepository.findByRole(Role.MODERATOR)) {
            notifyUser(moderator, Type.REPORT_RESUBMITTED, report,
                    "Report updated by creator", "Report " + report.getCode()
                            + " was edited and is ready for review again.");
        }
        notifyUser(user, Type.REPORT_RESUBMITTED, report,
                "Report resubmitted", "Your report was resubmitted for review.");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto verify(String id, User user) {
        Report report = load(id);
        lifecycle.apply(report, LifecycleAction.VERIFY, user, null, null);
        report = reportRepository.save(report);
        auditReport("REPORT_VERIFIED", user, report);
        notifyUser(report.getUser(), Type.REPORT_VERIFIED, report,
                "Report verified", "Your report has been verified.");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto reject(String id, User user, RejectReportRequest request) {
        Report report = load(id);
        lifecycle.apply(report, LifecycleAction.REJECT, user, request.comment(),
                request.reasonCode());
        report = reportRepository.save(report);
        auditReport("REPORT_REJECTED", user, report);
        notifyUser(report.getUser(), Type.REPORT_REJECTED, report,
                "Report rejected", request.comment());
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto markDuplicate(String id, User user, DuplicateReportRequest request) {
        Report report = load(id);
        Report canonical = reportRepository.findByCode(request.canonicalReportRef())
                .or(() -> reportRepository.findById(request.canonicalReportRef()))
                .orElseThrow(() -> new ResourceNotFoundException("Report",
                        request.canonicalReportRef()));
        if (canonical.getId().equals(report.getId())) {
            throw new IllegalArgumentException("A report cannot be a duplicate of itself.");
        }
        lifecycle.apply(report, LifecycleAction.MARK_DUPLICATE, user, null, null);
        report.setDuplicateOf(canonical);
        report = reportRepository.save(report);
        canonical.getStatusHistory().add(new com.mtaafix.report.domain.ReportStatusHistory(
                canonical, canonical.getStatus(), canonical.getStatus(), user,
                "Duplicate report " + report.getCode() + " was linked to this report.", "DUPLICATE_LINK"));
        reportRepository.save(canonical);
        auditReport("REPORT_DUPLICATE", user, report);
        notifyUser(report.getUser(), Type.REPORT_DUPLICATE, report,
                "Report closed as duplicate",
                "This report duplicates " + canonical.getCode() + ".");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto assign(String id, User user, String assigneeId, String organisationId) {
        Report report = load(id);
        boolean reassign = report.getStatus() == Status.ASSIGNED;
        lifecycle.apply(report, LifecycleAction.ASSIGN, user, null, null);
        assertInOrganisationScope(report, user);
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", assigneeId));
        if (assignee.getRole() != Role.FIELD_WORKER) {
            throw new IllegalArgumentException("Assignee must be a FIELD_WORKER.");
        }
        Organisation org = resolveOrganisation(organisationId, assignee, user);
        if (report.getAssignment() != null) {
            Assignment prior = report.getAssignment();
            prior.setStatus(Assignment.Status.CANCELLED);
            assignmentRepository.save(prior);
        }
        if (org != null) {
            report.setOrganisation(org);
        }
        report.setAssignment(assignmentRepository.save(new Assignment(report, assignee, org)));
        report = reportRepository.save(report);
        auditReport("REPORT_ASSIGNED", user, report);
        notifyUser(assignee, Type.REPORT_ASSIGNED, report,
                reassign ? "New assignment (reassigned)" : "New assignment",
                "Report " + report.getCode() + " has been assigned to you.");
        notifyUser(report.getUser(), Type.REPORT_ASSIGNED, report,
                "Report assigned", "An organization was assigned to your report.");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto startWork(String id, User user) {
        Report report = load(id);
        assertCanWork(report, user);
        lifecycle.apply(report, LifecycleAction.START_WORK, user, null, null);
        if (report.getAssignment() != null) {
            report.getAssignment().setStatus(Assignment.Status.IN_PROGRESS);
        }
        report = reportRepository.save(report);
        notifyUser(report.getUser(), Type.REPORT_IN_PROGRESS, report,
                "Work started", "Work has started on your report.");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto complete(String id, User user, CompleteReportRequest request) {
        Report report = load(id);
        assertCanWork(report, user);
        lifecycle.validate(report, LifecycleAction.SUBMIT_COMPLETION, user);
        boolean hasPhoto = report.getMedia().stream()
                .anyMatch(m -> m.getMediaType() == ReportMedia.MediaType.PHOTO);
        if (!hasPhoto) {
            throw new IllegalArgumentException(
                    "A completion claim requires at least one attached photo (T11 evidence).");
        }
        lifecycle.apply(report, LifecycleAction.SUBMIT_COMPLETION, user,
                request.completionNote(), null);
        if (report.getAssignment() != null) {
            report.getAssignment().setStatus(Assignment.Status.COMPLETED);
            report.getAssignment().setCompletedAt(Instant.now());
            assignmentRepository.save(report.getAssignment());
        }
        report = reportRepository.save(report);
        auditReport("REPORT_COMPLETED", user, report);
        for (User moderator : userRepository.findByRole(Role.MODERATOR)) {
            notifyUser(moderator, Type.REPORT_COMPLETED, report,
                    "Completion claim awaiting review",
                    "Report " + report.getCode() + " claims completion and needs review.");
        }
        notifyUser(report.getUser(), Type.REPORT_COMPLETED, report,
                "Completion claimed", "The assigned worker claims the issue is fixed.");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto returnToWork(String id, User user, ReturnReportRequest request) {
        Report report = load(id);
        lifecycle.apply(report, LifecycleAction.RETURN_TO_WORK, user, request.comment(), null);
        if (report.getAssignment() != null) {
            report.getAssignment().setStatus(Assignment.Status.IN_PROGRESS);
        }
        report = reportRepository.save(report);
        auditReport("REPORT_RETURNED", user, report);
        if (report.getAssignment() != null) {
            notifyUser(report.getAssignment().getAssignee(), Type.REPORT_RETURNED, report,
                    "Returned to you", request.comment());
        }
        notifyUser(report.getUser(), Type.REPORT_RETURNED, report,
                "More work needed", request.comment());
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto resolve(String id, User user) {
        Report report = load(id);
        lifecycle.apply(report, LifecycleAction.RESOLVE, user, null, null);
        report = reportRepository.save(report);
        auditReport("REPORT_RESOLVED", user, report);
        notifyUser(report.getUser(), Type.REPORT_RESOLVED, report,
                "Report resolved", "The resolution awaits your confirmation.");
        if (report.getAssignment() != null) {
            notifyUser(report.getAssignment().getAssignee(), Type.REPORT_RESOLVED, report,
                    "Resolution approved", "Your completion claim was approved.");
        }
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto close(String id, User user) {
        Report report = load(id);
        lifecycle.apply(report, LifecycleAction.CLOSE, user, null, null);
        report = reportRepository.save(report);
        notifyUser(report.getUser(), Type.REPORT_CLOSED, report,
                "Report closed", "The report has been closed.");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto confirm(String id, User user, ConfirmReportRequest request) {
        Report report = load(id);
        if (user.getRole() == Role.CITIZEN) {
            assertCreator(report, user, "Only the report creator can confirm this report.");
        }
        lifecycle.apply(report, LifecycleAction.CONFIRM, user, request != null ? request.feedback() : null, null);
        report = reportRepository.save(report);
        notifyUser(report.getUser(), Type.REPORT_CLOSED, report,
                "Report confirmed and closed", "Thank you for confirming the resolution.");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto dispute(String id, User user, DisputeReportRequest request) {
        Report report = load(id);
        if (user.getRole() == Role.CITIZEN) {
            assertCreator(report, user, "Only the report creator can dispute this report.");
        }
        lifecycle.apply(report, LifecycleAction.DISPUTE, user, request.disputeReason(), null);
        if (report.getAssignment() != null) {
            report.getAssignment().setStatus(Assignment.Status.IN_PROGRESS);
        }
        report = reportRepository.save(report);
        if (report.getAssignment() != null) {
            notifyUser(report.getAssignment().getAssignee(), Type.REPORT_DISPUTED, report,
                    "Resolution disputed", request.disputeReason());
        }
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto reopen(String id, User user, ReopenReportRequest request) {
        Report report = load(id);
        lifecycle.apply(report, LifecycleAction.REOPEN, user, request.justification(), null);
        report = reportRepository.save(report);
        auditReport("REPORT_REOPENED", user, report);
        notifyUser(report.getUser(), Type.REPORT_REOPENED, report,
                "Report reopened", request.justification());
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto update(String id, UpdateReportRequest request, User user) {
        Report report = load(id);
        if (!user.getId().equals(report.getUser().getId())) {
            throw new AccessDeniedException("Only the report creator can edit this report.");
        }
        if (report.getStatus() != Status.SUBMITTED && report.getStatus() != Status.NEEDS_INFORMATION) {
            throw new IllegalStateException(
                    "Reports can only be edited while SUBMITTED or NEEDS_INFORMATION.");
        }
        if (request.title() != null) report.setTitle(request.title());
        if (request.description() != null) report.setDescription(request.description());
        if (request.location() != null) report.setLocation(request.location().toPoint());
        if (request.categoryId() != null) {
            ReportCategory category = reportRepository.findCategoryById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
            report.setCategory(category);
        }
        if (request.score() != null) report.setScore(request.score());
        report = reportRepository.save(report);
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto addComment(String id, User user, CreateCommentRequest request) {
        Report report = load(id);
        assertCanRead(report, user);
        report.getComments().add(new ReportComment(request.content(), report, user));
        report = reportRepository.save(report);
        if (!user.getId().equals(report.getUser().getId())) {
            notifyUser(report.getUser(), Type.REPORT_COMMENTED, report,
                    "New comment", request.content());
        }
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto addMedia(String id, User user, String fileName, String originalName,
            String contentType, Long sizeBytes, String url, String mediaType) {
        Report report = load(id);
        assertCanRead(report, user);
        if (contentType == null || !ALLOWED_MEDIA_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported media MIME type; allowed: image/jpeg, image/png, image/webp (FR-9.2).");
        }
        if (sizeBytes == null || sizeBytes <= 0 || sizeBytes > MAX_MEDIA_BYTES) {
            throw new IllegalArgumentException(
                    "Media size must be between 1 byte and 20 MiB (FR-9.2).");
        }
        ReportMedia.MediaType mt;
        try {
            mt = ReportMedia.MediaType.valueOf(mediaType.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Unknown media type: " + mediaType);
        }
        if (mt != ReportMedia.MediaType.PHOTO) {
            throw new IllegalArgumentException(
                    "Only PHOTO media is accepted; allowed MIME types are image/jpeg, image/png, image/webp.");
        }
        report.getMedia().add(
                new ReportMedia(fileName, originalName, contentType, sizeBytes, url, mt, report, user));
        report = reportRepository.save(report);
        return ReportMapper.toDto(report);
    }

    @Transactional
    public void deleteMedia(String mediaId, User user) {
        ReportMedia media = reportRepository.findMediaById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", mediaId));
        assertCanRead(media.getReport(), user);
        boolean privileged = user.getRole() == Role.MODERATOR || user.getRole() == Role.ADMINISTRATOR;
        boolean uploader = media.getUploadedBy() != null
                && user.getId().equals(media.getUploadedBy().getId());
        boolean legacyOwner = media.getUploadedBy() == null
                && user.getId().equals(media.getReport().getUser().getId());
        if (!privileged && !uploader && !legacyOwner) {
            throw new AccessDeniedException("Only the uploader can remove this media.");
        }
        media.getReport().getMedia().remove(media);
        reportRepository.save(media.getReport());
    }

    private Report load(String id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
    }

    private void assertCreator(Report report, User user, String message) {
        if (!user.getId().equals(report.getUser().getId())) {
            throw new AccessDeniedException(message);
        }
    }

    private void assertCanWork(Report report, User user) {
        if (user.getRole() == Role.FIELD_WORKER) {
            if (report.getAssignment() == null || report.getAssignment().getAssignee() == null
                    || !user.getId().equals(report.getAssignment().getAssignee().getId())) {
                throw new AccessDeniedException("Report is not assigned to you.");
            }
            return;
        }
        if (user.getRole() == Role.ORGANIZATION_ADMIN) {
            assertInOrganisationScope(report, user);
        }
    }

    private Organisation resolveOrganisation(String organisationId, User assignee, User actor) {
        String resolved = organisationId;
        if (resolved == null && assignee.getOrganisationId() != null) {
            resolved = assignee.getOrganisationId();
        }
        if (resolved == null && actor.getRole() == Role.ORGANIZATION_ADMIN) {
            resolved = actor.getOrganisationId();
        }
        if (resolved == null) {
            return null;
        }
        final String orgId = resolved;
        return reportRepository.findOrganisationById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation", orgId));
    }

    private void auditReport(String action, User actor, Report report) {
        Map<String, String> details = new HashMap<>();
        details.put("reportCode", report.getCode());
        details.put("status", report.getStatus().name());
        auditService.record(action, actor, "Report", report.getId(), details);
    }

    private void notifyUser(User recipient, Type type, Report report, String title, String body) {
        if (recipient == null || recipient.getId() == null) {
            return;
        }
        notificationService.send(type, recipient, report, title, body);
    }

    private void assertCanRead(Report report, User user) {
        if (user.getRole() == Role.MODERATOR || user.getRole() == Role.ADMINISTRATOR) {
            return;
        }
        if (user.getId().equals(report.getUser().getId())) {
            return;
        }
        if (user.getRole() == Role.ORGANIZATION_ADMIN) {
            if (report.getOrganisation() != null && user.getOrganisationId() != null
                    && user.getOrganisationId().equals(report.getOrganisation().getId())) {
                return;
            }
            throw new AccessDeniedException("Report is outside your organisation scope.");
        }
        if (user.getRole() == Role.FIELD_WORKER) {
            if (report.getAssignment() != null && report.getAssignment().getAssignee() != null
                    && user.getId().equals(report.getAssignment().getAssignee().getId())) {
                return;
            }
            throw new AccessDeniedException("Report is not assigned to you.");
        }
        throw new AccessDeniedException("You can only access your own reports.");
    }

    private void assertInOrganisationScope(Report report, User user) {
        if (user.getRole() != Role.ORGANIZATION_ADMIN) {
            return;
        }
        if (report.getOrganisation() != null && user.getOrganisationId() != null
                && user.getOrganisationId().equals(report.getOrganisation().getId())) {
            return;
        }
        throw new AccessDeniedException("Report is outside your organisation scope.");
    }
}
