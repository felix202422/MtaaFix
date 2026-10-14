package com.mtaafix.report.service;

import com.mtaafix.report.domain.ReportCodeCounter;
import com.mtaafix.report.repository.ReportCodeCounterRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ReportCodeCounterInitializer {

    private final ReportCodeCounterRepository counterRepository;
    private final TransactionTemplate transactionTemplate;

    public ReportCodeCounterInitializer(ReportCodeCounterRepository counterRepository,
            PlatformTransactionManager transactionManager) {
        this.counterRepository = counterRepository;
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
        });
    }
}
