package com.sms.repository;

import com.sms.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    Optional<AcademicYear> findByIsCurrentTrue();
}
