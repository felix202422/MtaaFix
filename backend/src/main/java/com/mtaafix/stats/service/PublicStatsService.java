package com.mtaafix.stats.service;

import com.mtaafix.report.domain.Report;
import com.mtaafix.report.repository.ReportCategoryRepository;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.stats.dto.PublicStatsDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicStatsService {

    private final ReportRepository reportRepository;
    private final ReportCategoryRepository categoryRepository;

    public PublicStatsService(ReportRepository reportRepository,
            ReportCategoryRepository categoryRepository) {
        this.reportRepository = reportRepository;
        this.categoryRepository = categoryRepository;
    }

    /** Phase 17: TTL-cached (60s) — 4 COUNT queries per call was wasteful. */
    @org.springframework.cache.annotation.Cacheable(cacheNames = "publicStats")
    @Transactional(readOnly = true)
    public PublicStatsDto getStats() {
        return new PublicStatsDto(
                reportRepository.count(),
                reportRepository.countByStatus(Report.Status.SUBMITTED),
                reportRepository.countByStatus(Report.Status.RESOLVED),
                categoryRepository.count());
    }
}
