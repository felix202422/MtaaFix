package com.mtaafix.report.repository;

import com.mtaafix.report.domain.ReportCodeCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportCodeCounterRepository extends JpaRepository<ReportCodeCounter, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ReportCodeCounter c where c.year = :year")
    java.util.Optional<ReportCodeCounter> findByIdForUpdate(@Param("year") Integer year);
}
