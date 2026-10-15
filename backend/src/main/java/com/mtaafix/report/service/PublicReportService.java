package com.mtaafix.report.service;

import com.mtaafix.common.exceptions.ResourceNotFoundException;
import com.mtaafix.common.geo.GeoBounds;
import com.mtaafix.common.geo.LocationGeneralizer;
import com.mtaafix.common.geo.NearSearch;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicReportService {

    private static final Instant MAX_INSTANT = Instant.parse("9999-12-31T23:59:59Z");

    private final ReportRepository reportRepository;
    private final LocationGeneralizer locationGeneralizer;

    public PublicReportService(ReportRepository reportRepository,
            LocationGeneralizer locationGeneralizer) {
        this.reportRepository = reportRepository;
        this.locationGeneralizer = locationGeneralizer;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PublicReportSummaryDto> list(String status, String categoryId, String q,
            String dateFrom, String dateTo, String bbox, String near, Pageable pageable) {
        Status statusEnum = parseStatus(status);
        Instant from = parseDate(dateFrom, false);
        Instant to = parseDate(dateTo, true);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("dateFrom must be before dateTo.");
        }
        GeoBounds bounds = GeoBounds.parse(bbox);
        NearSearch nearSearch = NearSearch.parse(near);
        if (bounds != null && nearSearch != null) {
            throw new IllegalArgumentException("bbox and near filters cannot be combined.");
        }
        // Bind non-null date bounds: PostgreSQL cannot infer the type of a null
        // Instant parameter used in an `is null` check (H2 tolerates it).
        Instant fromBound = from != null ? from : Instant.EPOCH;
        Instant toBound = to != null ? to : MAX_INSTANT;
        String pattern = (q == null || q.isBlank()) ? null
                : "%" + q.trim().toLowerCase() + "%";
        String statusValue = statusEnum != null ? statusEnum.name() : null;
        Page<Report> page;
        if (bounds != null) {
            // Native query: order-by is declared in the SQL. A Sort on the Pageable
            // (e.g. reportedAt) cannot be resolved against the r.* alias and Hibernate
            // appends an unqualified `order by reportedat`, so strip any sort here.
            page = reportRepository.findPublicInBbox(statusValue, categoryId, pattern,
                    fromBound, toBound, bounds.minLongitude(), bounds.minLatitude(),
                    bounds.maxLongitude(), bounds.maxLatitude(), unsorted(pageable));
        } else if (nearSearch != null) {
            page = reportRepository.findPublicNear(statusValue, categoryId, pattern,
                    fromBound, toBound, nearSearch.latitude(), nearSearch.longitude(),
                    nearSearch.radiusMeters(), unsorted(pageable));
        } else {
            page = reportRepository.findPublic(statusEnum, categoryId, pattern, fromBound,
                    toBound, pageable);
        }
        return PagedResponse.of((int) page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize(),
                page.getContent().stream()
                        .map(r -> PublicReportSummaryDto.from(r, locationGeneralizer))
                        .toList());
    }

    @Transactional(readOnly = true)
    public PublicReportDetailDto getByCode(String code) {
        Report report = reportRepository.findPublicByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Report", code));
        return PublicReportDetailDto.from(report, locationGeneralizer);
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

    /**
     * Preserve pagination but drop any sort. Native spatial queries declare their own
     * order-by; a Spring Data Sort referencing a Java property (reportedAt) cannot be
     * mapped onto the native alias and would be appended as invalid SQL.
     */
    private static Pageable unsorted(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        }
        return pageable;
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
