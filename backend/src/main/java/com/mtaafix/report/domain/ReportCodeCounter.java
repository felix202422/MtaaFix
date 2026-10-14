package com.mtaafix.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "report_code_counters")
public class ReportCodeCounter {

    @Id
    @Column(name = "code_year", nullable = false)
    private Integer year;

    @Column(name = "last_serial", nullable = false)
    private Long lastSerial = 0L;

    public ReportCodeCounter() {
    }

    public ReportCodeCounter(Integer year, Long lastSerial) {
        this.year = year;
        this.lastSerial = lastSerial;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Long getLastSerial() {
        return lastSerial;
    }

    public void setLastSerial(Long lastSerial) {
        this.lastSerial = lastSerial;
    }
}
