package com.mtaafix.report.repository;

import com.mtaafix.report.domain.Report;
import com.mtaafix.report.domain.ReportCategory;
import com.mtaafix.report.domain.ReportMedia;
import com.mtaafix.report.domain.Organisation;
import com.mtaafix.report.domain.Report.Status;
import java.util.List;
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
            left join r.assignment a
            where (:status is null or r.status = :status)
            and (:categoryId is null or r.category.id = :categoryId)
            and (:orgId is null or o.id = :orgId)
            and (:userId is null or r.user.id = :userId)
            and (:assigneeId is null or a.assignee.id = :assigneeId)
            """)
    Page<Report> findByScope(@Param("status") Status status,
            @Param("categoryId") String categoryId, @Param("orgId") String orgId,
            @Param("userId") String userId, @Param("assigneeId") String assigneeId,
            Pageable pageable);

    @Query("select c from ReportCategory c where c.id = :id")
    Optional<ReportCategory> findCategoryById(@Param("id") String id);

    Optional<Report> findByCode(String code);

    @Query("""
            select r from Report r
            left join fetch r.category
            where (:status is null or r.status = :status)
            and (:categoryId is null or r.category.id = :categoryId)
            and (:pattern is null or lower(r.title) like :pattern
                or lower(r.description) like :pattern)
            and r.reportedAt >= :dateFrom
            and r.reportedAt <= :dateTo
            """)
    Page<Report> findPublic(@Param("status") Status status,
            @Param("categoryId") String categoryId, @Param("pattern") String pattern,
            @Param("dateFrom") java.time.Instant dateFrom,
            @Param("dateTo") java.time.Instant dateTo, Pageable pageable);

    @Query("""
            select r from Report r
            left join fetch r.category
            left join fetch r.duplicateOf
            left join fetch r.statusHistory
            where r.code = :code
            """)
    Optional<Report> findPublicByCode(@Param("code") String code);

    @Query("select o from Organisation o where o.id = :id")
    Optional<Organisation> findOrganisationById(@Param("id") String id);

    @Query("select m from ReportMedia m where m.id = :id")
    Optional<ReportMedia> findMediaById(@Param("id") String id);

    long countByUserId(String userId);

    long countByUserIdAndStatus(String userId, Status status);

    @Query("""
            select r from Report r
            where r.user.id = :userId
            and r.status in (com.mtaafix.report.domain.Report.Status.NEEDS_INFORMATION,
                com.mtaafix.report.domain.Report.Status.RESOLVED)
            order by r.updatedAt desc
            """)
    List<Report> findAttentionReportsByUserId(@Param("userId") String userId);

    @Query("select count(r) from Report r where r.user.id = :userId "
            + "and r.status <> com.mtaafix.report.domain.Report.Status.CLOSED "
            + "and r.status <> com.mtaafix.report.domain.Report.Status.REJECTED "
            + "and r.status <> com.mtaafix.report.domain.Report.Status.DUPLICATE")
    long countActiveByUserId(@Param("userId") String userId);

    long countByOrganisationId(String organisationId);

    long countByOrganisationIdAndStatus(String organisationId, Status status);

    long countByStatus(Status status);

    @Query("select r from Report r left join fetch r.assignment a where a.assignee.id = :assigneeId")
    List<Report> findReportsByAssigneeId(@Param("assigneeId") String assigneeId);

    @Query("select r from Report r left join fetch r.assignment a where r.organisation.id = :organisationId")
    List<Report> findReportsByOrganisationIdWithAssignment(@Param("organisationId") String organisationId);
}
