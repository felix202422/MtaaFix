package com.mtaafix.report.domain;

import com.mtaafix.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "report_status_history", indexes = {
        @Index(name = "idx_report_status_history_report", columnList = "report_id")
})
public class ReportStatusHistory extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 30)
    private Report.Status fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private Report.Status toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    public ReportStatusHistory() {
    }

    public ReportStatusHistory(Report report, Report.Status fromStatus, Report.Status toStatus) {
        this.report = report;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public Report.Status getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(Report.Status fromStatus) {
        this.fromStatus = fromStatus;
    }

    public Report.Status getToStatus() {
        return toStatus;
    }

    public void setToStatus(Report.Status toStatus) {
        this.toStatus = toStatus;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
