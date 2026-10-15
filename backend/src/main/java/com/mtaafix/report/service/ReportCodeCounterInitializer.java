package com.mtaafix.report.service;

import com.mtaafix.report.domain.ReportCodeCounter;
import com.mtaafix.report.repository.ReportCodeCounterRepository;
import com.mtaafix.report.repository.ReportRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ReportCodeCounterInitializer {

    private final ReportCodeCounterRepository counterRepository;
    private final ReportRepository reportRepository;
    private final TransactionTemplate transactionTemplate;

    public ReportCodeCounterInitializer(ReportCodeCounterRepository counterRepository,
            ReportRepository reportRepository,
            PlatformTransactionManager transactionManager) {
        this.counterRepository = counterRepository;
        this.reportRepository = reportRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @PostConstruct
    void seedYearWindow() {
        int year = LocalDate.now().getYear();
        transactionTemplate.executeWithoutResult(status -> {
            for (int y = year - 1; y <= year + 1; y++) {
                if (!counterRepository.existsById(y)) {
                    counterRepository.save(new ReportCodeCounter(y, 0L));
                }
            }
            bootstrapFromExistingReports(year);
        });
    }

    /**
     * If reports already exist with codes for this year but the counter is behind them
     * (e.g. codes were seeded or imported directly), advance the counter past the highest
     * existing serial so new codes never collide with a unique constraint.
     */
    private void bootstrapFromExistingReports(int year) {
        Integer maxSerial = reportRepository.findMaxReportSerial();
        if (maxSerial == null) {
            return;
        }
        counterRepository.findByIdForUpdate(year)
                .ifPresent(counter -> {
                    if (counter.getLastSerial() < maxSerial) {
                        counter.setLastSerial(maxSerial.longValue());
                        counterRepository.save(counter);
                    }
                });
    }
}
