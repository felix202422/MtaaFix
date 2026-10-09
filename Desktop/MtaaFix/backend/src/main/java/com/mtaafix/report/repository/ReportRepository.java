package com.mtaafix.report.repository;

import com.mtaafix.report.domain.Report;
import com.mtaafix.report.domain.ReportCategory;
import com.mtaafix.report.domain.ReportMedia;
import com.mtaafix.report.domain.Organisation;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, String> {

    @Query("""
            select r from Report r
            left join fetch r.category
            left join fetch r.organisation o
            where (:status is null or r.status = :status)
            and (:categoryId is null or r.category.id = :categoryId)
            and (:orgId is null or o.id = :orgId)
            """)
    Page<Report> findByStatusAndCategoryIdAndOrganisationId(@Param("status") String status,
            @Param("categoryId") String categoryId, @Param("orgId") String orgId, Pageable pageable);

    @Query("select c from ReportCategory c where c.id = :id")
    Optional<ReportCategory> findCategoryById(@Param("id") String id);

    @Query("select o from Organisation o where o.id = :id")
    Optional<Organisation> findOrganisationById(@Param("id") String id);

    @Query("select m from ReportMedia m where m.id = :id")
    Optional<ReportMedia> findMediaById(@Param("id") String id);
}
