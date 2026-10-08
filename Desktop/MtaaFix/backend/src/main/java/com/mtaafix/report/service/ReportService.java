package com.mtaafix.report.service;

import com.mtaafix.common.exceptions.ResourceNotFoundException;
import com.mtaafix.common.response.PagedResponse;
import com.mtaafix.notification.domain.Notification;
import com.mtaafix.notification.domain.Notification.Type;
import com.mtaafix.notification.dto.NotificationDto;
import com.mtaafix.notification.service.NotificationService;
import com.mtaafix.report.domain.Report;
import com.mtaafix.report.domain.Report.Status;
import com.mtaafix.report.domain.ReportStatusHistory;
import com.mtaafix.report.domain.ReportCategory;
import com.mtaafix.report.domain.ReportComment;
import com.mtaafix.report.domain.ReportMedia;
import com.mtaafix.report.domain.Assignment;
import com.mtaafix.report.dto.CreateCommentRequest;
import com.mtaafix.report.dto.CreateMediaRequest;
import com.mtaafix.report.dto.CreateReportRequest;
import com.mtaafix.report.dto.LocationDto;
import com.mtaafix.report.dto.ReportDto;
import com.mtaafix.report.dto.ReportMediaDto;
import com.mtaafix.report.dto.StatusHistoryDto;
import com.mtaafix.report.dto.UpdateReportRequest;
import com.mtaafix.report.mapper.ReportMapper;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.domain.User.Role;
import com.mtaafix.user.repository.UserRepository;
import java.util.List;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ReportService(ReportRepository reportRepository, UserRepository userRepository,
            NotificationService notificationService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public ReportDto create(CreateReportRequest request, User creator) {
        Report report = ReportMapper.toEntity(request);
        report.setUser(creator);
        report.setStatus(Status.SUBMITTED);
        report = reportRepository.save(report);
        notificationService.send(Type.REPORT_SUBMITTED, creator, report,
                "New report submitted", "A new report needs review.");
        return ReportMapper.toDto(report);
    }

    public ReportDto get(String id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        return ReportMapper.toDto(report);
    }

    public PagedResponse<ReportDto> list(String status, String categoryId, String orgId, Pageable pageable) {
        Page<Report> page;
        if (status == null && categoryId == null && orgId == null) {
            page = reportRepository.findAll(pageable);
        } else {
            page = reportRepository.findByStatusAndCategoryIdAndOrganisationId(status, categoryId, orgId, pageable);
        }
        List<ReportDto> content = page.getContent().stream()
                .map(ReportMapper::toDto)
                .toList();
        return PagedResponse.of(page.getTotalElements(), page.getTotalPages(), page.getNumber(),
                page.getSize(), content);
    }

    @Transactional
    public ReportDto submit(String id, User user) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        if (!report.getStatus().equals(Status.SUBMITTED)) {
            throw new IllegalStateException("Only SUBMITTED reports can be submitted.");
        }
        report.setStatus(Status.UNDER_REVIEW);
        report.getStatusHistory().add(new ReportStatusHistory(report, Status.SUBMITTED, Status.UNDER_REVIEW));
        report = reportRepository.save(report);
        notificationService.send(Type.REPORT_SUBMITTED, user, report,
                "Your report is under review", "Moderator review is in progress.");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto verify(String id, User user) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        if (!report.getStatus().equals(Status.UNDER_REVIEW)) {
            throw new IllegalStateException("Only UNDER_REVIEW reports can be verified.");
        }
        report.setStatus(Status.VERIFIED);
        report.getStatusHistory().add(new ReportStatusHistory(report, Status.UNDER_REVIEW, Status.VERIFIED));
        report = reportRepository.save(report);
        notificationService.send(Type.REPORT_VERIFIED, user, report,
                "Report verified", "Your report has been verified.");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto reject(String id, User user, String comment) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        if (!report.getStatus().equals(Status.UNDER_REVIEW)) {
            throw new IllegalStateException("Only UNDER_REVIEW reports can be rejected.");
        }
        report.setStatus(Status.REJECTED);
        report.getStatusHistory().add(new ReportStatusHistory(report, Status.UNDER_REVIEW, Status.REJECTED, user, comment));
        report = reportRepository.save(report);
        notificationService.send(Type.REPORT_REJECTED, user, report,
                "Report rejected", "Your report was rejected.");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto assign(String id, User user, String assigneeId, String organisationId) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        if (!report.getStatus().equals(Status.VERIFIED)) {
            throw new IllegalStateException("Only VERIFIED reports can be assigned.");
        }
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", assigneeId));
        if (organisationId != null) {
            Organisation org = orgByRole(assignee);
            report.setOrganisation(org != null ? org : null);
        }
        report.setStatus(Status.ASSIGNED);
        report.getStatusHistory().add(new ReportStatusHistory(report, Status.VERIFIED, Status.ASSIGNED));
        report.setAssignment(new Assignment(report, assignee, null));
        report = reportRepository.save(report);
        notificationService.send(Type.REPORT_ASSIGNED, user, report,
                "New assignment", "You have been assigned this report.");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto resolve(String id, User user) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        if (!report.getStatus().equals(Status.ASSIGNED)) {
            throw new IllegalStateException("Only ASSIGNED reports can be resolved.");
        }
        report.setStatus(Status.RESOLVED);
        report.getStatusHistory().add(new ReportStatusHistory(report, Status.ASSIGNED, Status.RESOLVED));
        report = reportRepository.save(report);
        notificationService.send(Type.REPORT_RESOLVED, user, report,
                "Report resolved", "The issue has been resolved.");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto close(String id, User user) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        if (!report.getStatus().equals(Status.RESOLVED)) {
            throw new IllegalStateException("Only RESOLVED reports can be closed.");
        }
        report.setStatus(Status.CLOSED);
        report.getStatusHistory().add(new ReportStatusHistory(report, Status.RESOLVED, Status.CLOSED));
        report = reportRepository.save(report);
        notificationService.send(Type.REPORT_CLOSED, user, report,
                "Report closed", "The report has been closed.");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto update(String id, UpdateReportRequest request, User user) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        if (report.getStatus() == Status.ASSIGNED) {
            throw new IllegalStateException("Cannot update assigned reports.");
        }
        if (request.subject() != null) report.setSubject(request.subject());
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
    public ReportDto addComment(String id, User user, String content) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        report.getComments().add(new ReportComment(content, report, user));
        report = reportRepository.save(report);
        notificationService.send(Type.REPORT_VERIFIED, user, report,
                "New comment", "A new comment was added to the report.");
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto addMedia(String id, User user, String fileName, String originalName,
            String contentType, Long sizeBytes, String url, String mediaType) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        ReportMedia.MediaType mt = ReportMedia.MediaType.valueOf(mediaType.toUpperCase());
        report.getMedia().add(new ReportMedia(fileName, originalName, contentType, (int) sizeBytes, url, mt, report));
        report = reportRepository.save(report);
        notificationService.send(Type.REPORT_VERIFIED, user, report,
                "New evidence", "Evidence was added to the report.");
        return ReportMapper.toDto(report);
    }

    public void deleteMedia(String mediaId, User user) {
        ReportMedia media = reportRepository.findMediaById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", mediaId));
        media.getReport().getMedia().remove(media);
        reportRepository.save(media.getReport());
    }

    private Organisation orgByRole(User user) {
        if (user.getRole() == Role.ORGANIZATION_ADMIN && user.getOrganisationId() != null) {
            return reportRepository.findOrganisationById(user.getOrganisationId())
                    .orElse(null);
        }
        return null;
    }
}
