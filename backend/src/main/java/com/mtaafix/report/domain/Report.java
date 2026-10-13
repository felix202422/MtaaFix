package com.mtaafix.report.domain;

import com.mtaafix.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import com.mtaafix.user.domain.User;
import com.mtaafix.report.domain.Assignment;
import com.mtaafix.report.domain.Organisation;
import com.mtaafix.report.domain.ReportCategory;

@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_reports_status", columnList = "status"),
        @Index(name = "idx_reports_category", columnList = "category_id"),
        @Index(name = "idx_reports_organisation", columnList = "organisation_id"),
        @Index(name = "idx_reports_subject", columnList = "subject"),
        @Index(name = "idx_reports_user", columnList = "user_id")
})
public class Report extends BaseEntity {

    public enum Status {
        SUBMITTED,
        UNDER_REVIEW,
        VERIFIED,
        ASSIGNED,
        IN_PROGRESS,
        RESOLVED,
        REJECTED,
        CLOSED
    }

    public enum Subject {
        POTHOLE,
        BROKEN_STREETSIDE_LIGHT,
        DAMAGED_ROAD,
        GARBAGE_ACCUMULATION,
        BLOCKED_DRAINAGE,
        WATER_LEAK,
        FLOODING,
        DAMAGED_PUBLIC_INFRASTRUCTURE,
        FALLEN_TREE,
        DAMAGED_ELECTRICAL_INFRASTRUCTURE,
        UNSAFE_PUBLIC_AREA,
        OTHER
    }

    @Column(name = "subject", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Subject subject;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "location", columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(name = "location_area", columnDefinition = "geometry(Polygon, 4326)")
    private Polygon locationArea;

    @Column(name = "reported_at", nullable = false, columnDefinition = "timestamp with time zone")
    private java.time.Instant reportedAt = java.time.Instant.now();

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Status status = Status.SUBMITTED;

    @Column(name = "score", nullable = false)
    private Integer score = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ReportCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportStatusHistory> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportMedia> media = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportComment> comments = new ArrayList<>();

    public Report() {
    }

    public Report(Subject subject, String title, String description, Point location,
            Polygon locationArea, User user) {
        this.subject = subject;
        this.title = title;
        this.description = description;
        this.location = location;
        this.locationArea = locationArea;
        this.user = user;
        this.statusHistory.add(new ReportStatusHistory(this, null, Status.SUBMITTED));
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public String getTitle() {
        return title;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public Polygon getLocationArea() {
        return locationArea;
    }

    public void setLocationArea(Polygon locationArea) {
        this.locationArea = locationArea;
    }

    public java.time.Instant getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(java.time.Instant reportedAt) {
        this.reportedAt = reportedAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ReportCategory getCategory() {
        return category;
    }

    public void setCategory(ReportCategory category) {
        this.category = category;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }

    public List<ReportStatusHistory> getStatusHistory() {
        return statusHistory;
    }

    public List<ReportMedia> getMedia() {
        return media;
    }

    public List<ReportComment> getComments() {
        return comments;
    }
}
