package com.mtaafix.report.domain;

import com.mtaafix.common.domain.BaseEntity;
import com.mtaafix.report.domain.Report;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "report_media", indexes = {
        @Index(name = "idx_report_media_report", columnList = "report_id")
})
public class ReportMedia extends BaseEntity {

    public enum MediaType {
        PHOTO,
        VIDEO,
        DOCUMENT
    }

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 30)
    private MediaType mediaType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private com.mtaafix.user.domain.User uploadedBy;

    public ReportMedia() {
    }

    public ReportMedia(String fileName, String originalName, String contentType, Long sizeBytes,
            String url, MediaType mediaType, Report report) {
        this.fileName = fileName;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.url = url;
        this.mediaType = mediaType;
        this.report = report;
    }

    public ReportMedia(String fileName, String originalName, String contentType, Long sizeBytes,
            String url, MediaType mediaType, Report report, com.mtaafix.user.domain.User uploadedBy) {
        this(fileName, originalName, contentType, sizeBytes, url, mediaType, report);
        this.uploadedBy = uploadedBy;
    }

    public com.mtaafix.user.domain.User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(com.mtaafix.user.domain.User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }
}
