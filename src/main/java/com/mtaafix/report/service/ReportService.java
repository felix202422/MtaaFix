package com.mtaafix.report.service;

import com.mtaafix.common.exceptions.ResourceNotFoundException;
import com.mtaafix.report.domain.Report;
import com.mtaafix.report.domain.Report.Status;
import com.mtaafix.report.domain.ReportStatusHistory;
import com.mtaafix.report.domain.User;
import com.mtaafix.report.dto.CreateReportRequest;
import com.mtaafix.report.dto.ReportDto;
import com.mtaafix.report.mapper.ReportMapper;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.user.domain.User.Role;
import com.mtaafix.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    public ReportService(ReportRepository reportRepository, UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    public ReportDto create(CreateReportRequest request, User creator) {
        Report report = ReportMapper.toEntity(request);
        report.setUser(creator);
        report = reportRepository.save(report);
        return ReportMapper.toDto(report);
    }

    public ReportDto get(String id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        return ReportMapper.toDto(report);
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
        return ReportMapper.toDto(report);
    }

    @Transactional
    public ReportDto assign(String id, User user, String assigneeId) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        if (!report.getStatus().equals(Status.VERIFIED)) {
            throw new IllegalStateException("Only VERIFIED reports can be assigned.");
        }
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", assigneeId));
        report.setStatus(Status.ASSIGNED);
        report.getStatusHistory().add(new ReportStatusHistory(report, Status.VERIFIED, Status.ASSIGNED));
        report.setAssignment(new com.mtaafix.report.domain.Assignment(report, assignee, null));
        report = reportRepository.save(report);
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
        return ReportMapper.toDto(report);
    }
}
