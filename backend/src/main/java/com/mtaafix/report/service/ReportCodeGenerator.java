package com.mtaafix.report.service;

import com.mtaafix.report.domain.ReportCodeCounter;
import com.mtaafix.report.repository.ReportCodeCounterRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportCodeGenerator {

    private final ReportCodeCounterRepository counterRepository;

    public ReportCodeGenerator(ReportCodeCounterRepository counterRepository) {
        this.counterRepository = counterRepository;
    }

    @Transactional
    public String next() {
        int year = LocalDate.now().getYear();
        ReportCodeCounter counter = counterRepository.findByIdForUpdate(year)
                .orElseGet(() -> counterRepository.save(new ReportCodeCounter(year, 0L)));
        long serial = counter.getLastSerial() + 1;
        counter.setLastSerial(serial);
        counterRepository.save(counter);
        return String.format("MTF-%d-%06d", year, serial);
    }
}
