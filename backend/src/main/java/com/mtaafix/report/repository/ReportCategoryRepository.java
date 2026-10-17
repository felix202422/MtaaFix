package com.mtaafix.report.repository;

import com.mtaafix.report.domain.ReportCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportCategoryRepository extends JpaRepository<ReportCategory, String> {
    Optional<ReportCategory> findBySlug(String slug);
}
