package com.mtaafix.report.domain;

import com.mtaafix.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.mtaafix.report.domain.Organisation;
import com.mtaafix.report.domain.Report;
import com.mtaafix.user.domain.User;

@Entity
@Table(name = "assignments", indexes = {
        @Index(name = "idx_assignments_status", columnList = "status"),
        @Index(name = "idx_assignments_assignee", columnList = "assignee_id"),
        @Index(name = "idx_assignments_organisation", columnList = "organisation_id")
})
public class Assignment extends BaseEntity {

    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    @Column(name = "assigned_at", columnDefinition = "timestamp with time zone")
    private java.time.Instant assignedAt;

    @Column(name = "completed_at", columnDefinition = "timestamp with time zone")
    private java.time.Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status = Status.PENDING;

    @ManyToOne
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    public Assignment() {
    }

    public Assignment(Report report, User assignee, Organisation organisation) {
        this.report = report;
        this.assignee = assignee;
        this.organisation = organisation;
        this.status = Status.PENDING;
    }

    public java.time.Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(java.time.Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public java.time.Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(java.time.Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }
}
