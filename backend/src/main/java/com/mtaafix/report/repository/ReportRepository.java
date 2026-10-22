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

    // Phase 17 (NFR-7): assignment + assignee are join-FETCHed so list mapping
    // never lazy-loads them per row (staff lists read assigneeId). Media is
    // batch-loaded separately by ReportService.list (one IN-query per page);
    // statusHistory/comments are intentionally not fetched — list responses
    // carry empty arrays for them (detail endpoints keep the full mapping).
    @Query("""
            select r from Report r
            left join fetch r.category
            left join fetch r.organisation o
            left join fetch r.assignment a
            left join fetch a.assignee
            left join fetch a.organisation
            where (:status is null or r.status = :status)
            and (:categoryId is null or r.category.id = :categoryId)
            and (:orgId is null or o.id = :orgId)
            and (:userId is null or r.user.id = :userId)
            and (:assigneeId is null or a.assignee.id = :assigneeId)
            and (:pattern is null or lower(r.code) like :pattern
                or lower(r.title) like :pattern or lower(r.description) like :pattern)
            and (:assigned is null or (:assigned = true and a.id is not null)
                or (:assigned = false and a.id is null))
            """)
    Page<Report> findByScope(@Param("status") Status status,
            @Param("categoryId") String categoryId, @Param("orgId") String orgId,
            @Param("userId") String userId, @Param("assigneeId") String assigneeId,
            @Param("pattern") String pattern, @Param("assigned") Boolean assigned,
            Pageable pageable);

    /** Phase 17: one batch media load per list page (replaces per-row lazy loads). */
    @Query("select m from ReportMedia m where m.report.id in :ids")
    List<ReportMedia> findMediaByReportIds(@Param("ids") java.util.Collection<String> ids);

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

    // Spatial public filters (FR-11.3/FR-8.3): PostGIS native SQL using the GIST
    // index on reports.location. PostgreSQL-only — the H2 suite covers parameter
    // validation but never executes these queries.
    //
    // These queries must be called with an UNSORTED Pageable: the ordering is
    // declared in the SQL below. If the Pageable carries a Sort, Hibernate appends
    // a second `order by <property>` derived from the Java field name (e.g.
    // r.reportedAt), which is invalid against the native `r` alias — Postgres then
    // fails with `column r.reportedat does not exist`. PublicReportService strips
    // any incoming sort before calling these.
    @Query(value = """
            select r.* from reports r
            where (cast(:status as varchar) is null or r.status = :status)
            and (:categoryId is null or r.category_id = :categoryId)
            and (:pattern is null or lower(r.title) like :pattern
                or (r.description is not null and lower(r.description) like :pattern))
            and r.reported_at >= :dateFrom
            and r.reported_at <= :dateTo
            and r.location is not null
            and ST_Intersects(r.location,
                ST_MakeEnvelope(:minLongitude, :minLatitude, :maxLongitude, :maxLatitude, 4326))
            order by r.reported_at desc
            """, countQuery = """
            select count(*) from reports r
            where (cast(:status as varchar) is null or r.status = :status)
            and (:categoryId is null or r.category_id = :categoryId)
            and (:pattern is null or lower(r.title) like :pattern
                or (r.description is not null and lower(r.description) like :pattern))
            and r.reported_at >= :dateFrom
            and r.reported_at <= :dateTo
            and r.location is not null
            and ST_Intersects(r.location,
                ST_MakeEnvelope(:minLongitude, :minLatitude, :maxLongitude, :maxLatitude, 4326))
            """, nativeQuery = true)
    Page<Report> findPublicInBbox(@Param("status") String status,
            @Param("categoryId") String categoryId, @Param("pattern") String pattern,
            @Param("dateFrom") java.time.Instant dateFrom,
            @Param("dateTo") java.time.Instant dateTo,
            @Param("minLongitude") double minLongitude,
            @Param("minLatitude") double minLatitude,
            @Param("maxLongitude") double maxLongitude,
            @Param("maxLatitude") double maxLatitude, Pageable pageable);

    @Query(value = """
            select r.* from reports r
            where (cast(:status as varchar) is null or r.status = :status)
            and (:categoryId is null or r.category_id = :categoryId)
            and (:pattern is null or lower(r.title) like :pattern
                or (r.description is not null and lower(r.description) like :pattern))
            and r.reported_at >= :dateFrom
            and r.reported_at <= :dateTo
            and r.location is not null
            and ST_DWithin(r.location::geography,
                ST_MakePoint(:longitude, :latitude)::geography, :radiusMeters)
            order by r.reported_at desc
            """, countQuery = """
            select count(*) from reports r
            where (cast(:status as varchar) is null or r.status = :status)
            and (:categoryId is null or r.category_id = :categoryId)
            and (:pattern is null or lower(r.title) like :pattern
                or (r.description is not null and lower(r.description) like :pattern))
            and r.reported_at >= :dateFrom
            and r.reported_at <= :dateTo
            and r.location is not null
            and ST_DWithin(r.location::geography,
                ST_MakePoint(:longitude, :latitude)::geography, :radiusMeters)
            """, nativeQuery = true)
    Page<Report> findPublicNear(@Param("status") String status,
            @Param("categoryId") String categoryId, @Param("pattern") String pattern,
            @Param("dateFrom") java.time.Instant dateFrom,
            @Param("dateTo") java.time.Instant dateTo,
            @Param("latitude") double latitude, @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters, Pageable pageable);

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

    @Query("select max(cast(substring(r.code, 10) as int)) from Report r "
            + "where r.code like 'MTF-%'")
    Integer findMaxReportSerial();

    // Phase 17: assignment endpoints are paginated (NFR-7); assignee +
    // organisation are join-FETCHed so AssignmentDto mapping stays N+1-free.
    // Ordering is declared in the SQL; callers pass an unsorted Pageable
    // (same convention as the public spatial queries above).
    @Query(value = """
            select r from Report r
            left join fetch r.assignment a
            left join fetch a.assignee
            left join fetch a.organisation
            where a.assignee.id = :assigneeId
            order by a.assignedAt desc
            """, countQuery = """
            select count(r) from Report r
            where r.assignment.assignee.id = :assigneeId
            """)
    Page<Report> findReportsByAssigneeId(@Param("assigneeId") String assigneeId,
            Pageable pageable);

    @Query(value = """
            select r from Report r
            left join fetch r.assignment a
            left join fetch a.assignee
            left join fetch a.organisation
            where r.organisation.id = :organisationId
            order by a.assignedAt desc
            """, countQuery = """
            select count(r) from Report r where r.organisation.id = :organisationId
            """)
    Page<Report> findReportsByOrganisationIdWithAssignment(
            @Param("organisationId") String organisationId, Pageable pageable);

    // ---- Phase 9 admin dashboard / analytics (P14, P23) ----

    @Query("select r.status, count(r) from Report r group by r.status")
    List<Object[]> countGroupByStatus();

    long countByReportedAtAfter(java.time.Instant since);

    @Query("select r.reportedAt from Report r where r.reportedAt >= :since order by r.reportedAt asc")
    List<java.time.Instant> findReportedAtSince(@Param("since") java.time.Instant since);

    /**
     * Phase 17: dashboard activity bucketed by day in SQL (app zone), replacing
     * the load-every-Instant + Java bucketing path. PostgreSQL-only — the H2
     * test profile never runs Flyway and the service keeps the Java fallback
     * for non-PostgreSQL dialects.
     */
    @Query(value = """
            select (reported_at at time zone 'Africa/Dar_es_Salaam')::date as day,
                   count(*) as cnt
            from reports
            where reported_at >= :since
            group by 1
            order by 1
            """, nativeQuery = true)
    List<Object[]> countGroupedByAppDaySince(@Param("since") java.time.Instant since);

    @Query("select r.category.id, count(r) from Report r group by r.category.id")
    List<Object[]> countGroupByCategory();

    @Query("select r.status, count(r) from Report r where r.reportedAt >= :since group by r.status")
    List<Object[]> countGroupByStatusSince(@Param("since") java.time.Instant since);

    List<Report> findTop10ByStatusInOrderByReportedAtDesc(java.util.Collection<Status> statuses);

    List<Report> findTop5ByStatusOrderByReportedAtDesc(Status status);

    long countByAssignmentIsNull();

    long countByAssignmentIsNotNull();

    @Query("select r.organisation.id, count(r) from Report r "
            + "where r.assignment is not null group by r.organisation.id")
    List<Object[]> countAssignedGroupByOrganisation();

    @Query("select r.assignment.assignee.id, count(r) from Report r "
            + "where r.status in :statuses and r.assignment.assignee is not null "
            + "group by r.assignment.assignee.id")
    List<Object[]> countActiveWorkGroupByAssignee(@Param("statuses") java.util.Collection<Status> statuses);

    @Query("select r from Report r order by r.reportedAt desc")
    List<Report> findRecent(org.springframework.data.domain.Pageable pageable);
}
