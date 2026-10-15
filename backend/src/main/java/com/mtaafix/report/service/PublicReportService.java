package com.mtaafix.report.service;

import com.mtaafix.common.exceptions.ResourceNotFoundException;
import com.mtaafix.common.response.PagedResponse;
import com.mtaafix.report.domain.Report;
import com.mtaafix.report.domain.Report.Status;
import com.mtaafix.report.dto.PublicReportDetailDto;
import com.mtaafix.report.dto.PublicReportSummaryDto;
import com.mtaafix.report.repository.ReportRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicReportService {

    private static final Instant MAX_INSTANT = Instant.parse("9999-12-31T23:59:59Z");

    private final ReportRepository reportRepository;

    public PublicReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PublicReportSummaryDto> list(String status, String categoryId, String q,
            String dateFrom, String dateTo, Pageable pageable) {
        Status statusEnum = parseStatus(status);
        Instant from = parseDate(dateFrom, false);
        Instant to = parseDate(dateTo, true);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("dateFrom must be before dateTo.");
        }
        // Bind non-null date bounds: PostgreSQL cannot infer the type of a null
        // Instant parameter used in an `is null` check (H2 tolerates it).
        Instant fromBound = from != null ? from : Instant.EPOCH;
        Instant toBound = to != null ? to : MAX_INSTANT;
        String pattern = (q == null || q.isBlank()) ? null
                : "%" + q.trim().toLowerCase() + "%";
        Page<Report> page = reportRepository.findPublic(statusEnum, categoryId, pattern, fromBound,
                toBound, pageable);
        return PagedResponse.of((int) page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize(),
                page.getContent().stream().map(PublicReportSummaryDto::from).toList());
    }

    @Transactional(readOnly = true)
    public PublicReportDetailDto getByCode(String code) {
        Report report = reportRepository.findPublicByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Report", code));
        return PublicReportDetailDto.from(report);
    }

    private Status parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return Status.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown status filter: " + status);
        }
    }

    private Instant parseDate(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value);
            return endOfDay
                    ? date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                    : date.atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Invalid date filter (expected YYYY-MM-DD): " + value);
        }
    }
}
