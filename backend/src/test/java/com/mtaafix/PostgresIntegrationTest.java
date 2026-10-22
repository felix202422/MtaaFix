package com.mtaafix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mtaafix.report.domain.Report;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = MtaaFixApplication.class)
@EnabledIfEnvironmentVariable(named = "MTAAFIX_PG_IT", matches = "true")
@Transactional
class PostgresIntegrationTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private com.mtaafix.report.service.PublicReportService publicReportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.mtaafix.dashboard.service.AnalyticsService analyticsService;

    @Autowired
    private com.mtaafix.report.repository.IdempotencyKeyRepository idempotencyKeyRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @SuppressWarnings("unchecked")
    void flywayMigrationApplied() {
        List<Object[]> rows = entityManager
                .createNativeQuery("SELECT version, description, success FROM flyway_schema_history")
                .getResultList();
        assertTrue(rows.stream().anyMatch(r -> "1".equals(String.valueOf(r[0]))));
        assertTrue(rows.stream().anyMatch(r -> "2".equals(String.valueOf(r[0]))),
                "Phase 5 V2 migration must be applied");
        assertTrue(rows.stream().anyMatch(r -> "6".equals(String.valueOf(r[0]))),
                "Phase 14 V6 migration must be applied");
        assertTrue(rows.stream().anyMatch(r -> "7".equals(String.valueOf(r[0]))),
                "Phase 16 V7 migration must be applied");
        assertTrue(rows.stream().allMatch(r -> Boolean.TRUE.equals(r[2])));
    }

    @Test
    void postgisExtensionAvailable() {
        Object version = entityManager
                .createNativeQuery("SELECT postgis_version()")
                .getSingleResult();
        assertNotNull(version);
        assertTrue(version.toString().startsWith("3"));
    }

    @Test
    void spatialRoundTrip() {
        User user = new User();
        user.setEmail("it-" + System.nanoTime() + "@example.com");
        user.setPasswordHash("hash");
        user.setFullName("IT User");
        user.setRole(User.Role.CITIZEN);
        userRepository.save(user);

        org.locationtech.jts.geom.GeometryFactory gf =
                new org.locationtech.jts.geom.GeometryFactory();
        org.locationtech.jts.geom.Point point = gf.createPoint(
                new org.locationtech.jts.geom.Coordinate(39.2084, -6.8229));
        point.setSRID(4326);

        com.mtaafix.report.domain.ReportCategory category = entityManager.find(
                com.mtaafix.report.domain.ReportCategory.class,
                "5f0a0001-0000-4000-8000-000000000001");
        assertNotNull(category, "V2 must seed legacy-subject categories");

        Report report = new Report(category, "IT pothole", "round trip",
                point, null, user);
        report.setCode("MTF-IT-" + System.nanoTime());
        reportRepository.save(report);
        entityManager.flush();
        entityManager.clear();

        Report loaded = reportRepository.findById(report.getId()).orElseThrow();
        assertNotNull(loaded.getLocation());
        assertEquals(4326, loaded.getLocation().getSRID());
        assertEquals(39.2084, loaded.getLocation().getX(), 1e-9);
        assertEquals(-6.8229, loaded.getLocation().getY(), 1e-9);

        Object wkt = entityManager.createNativeQuery(
                        "SELECT ST_AsText(location) FROM reports WHERE id = ?1")
                .setParameter(1, report.getId())
                .getSingleResult();
        assertEquals("POINT(39.2084 -6.8229)", wkt);
    }

    @Test
    void gistIndexExistsOnReportsLocation() {
        @SuppressWarnings("unchecked")
        List<String> indexes = entityManager.createNativeQuery(
                        "SELECT indexname FROM pg_indexes "
                                + "WHERE tablename = 'reports' AND indexdef ILIKE '%GIST%'")
                .getResultList();
        assertTrue(indexes.stream().anyMatch("idx_reports_location"::equals),
                "FR-11.3 requires a GIST index on reports.location");
    }

    @Test
    @SuppressWarnings("unchecked")
    void phase9AdministrationMigrationApplied() {
        List<Object[]> versions = entityManager
                .createNativeQuery("SELECT version, success FROM flyway_schema_history")
                .getResultList();
        assertTrue(versions.stream().anyMatch(v -> "4".equals(String.valueOf(v[0]))),
                "Phase 9 V4 administration migration must be applied");
        assertTrue(versions.stream().allMatch(v -> Boolean.TRUE.equals(v[1])));

        Long dropped = (Long) entityManager.createNativeQuery(
                        "SELECT count(*) FROM information_schema.columns "
                                + "WHERE table_name = 'users' AND column_name = 'organisation_id'")
                .getSingleResult();
        assertEquals(0L, dropped,
                "users.organisation_id must be dropped in favour of organization_members (D-P9.1)");

        Long membership = (Long) entityManager.createNativeQuery(
                        "SELECT count(*) FROM information_schema.tables "
                                + "WHERE table_name = 'organization_members'")
                .getSingleResult();
        assertTrue(membership > 0, "organization_members table must exist (D-P9.1)");

        Long outcome = (Long) entityManager.createNativeQuery(
                        "SELECT count(*) FROM information_schema.columns "
                                + "WHERE table_name = 'audit_logs' AND column_name = 'outcome'")
                .getSingleResult();
        assertTrue(outcome > 0, "audit_logs.outcome must exist (FR-10 outcome field)");
    }

    @Test
    @SuppressWarnings("unchecked")
    void analyticsSummaryExecutesOnPostgres() {
        User owner = new User();
        owner.setEmail("analytics-it-" + System.nanoTime() + "@example.com");
        owner.setPasswordHash("hash");
        owner.setFullName("Analytics IT User");
        owner.setRole(User.Role.CITIZEN);
        userRepository.save(owner);

        User admin = new User();
        admin.setEmail("analytics-admin-" + System.nanoTime() + "@example.com");
        admin.setPasswordHash("hash");
        admin.setFullName("Analytics IT Admin");
        admin.setRole(User.Role.ADMINISTRATOR);
        userRepository.save(admin);

        com.mtaafix.report.domain.ReportCategory category = entityManager.find(
                com.mtaafix.report.domain.ReportCategory.class,
                "5f0a0001-0000-4000-8000-000000000001");
        assertNotNull(category);

        org.locationtech.jts.geom.GeometryFactory gf =
                new org.locationtech.jts.geom.GeometryFactory();
        org.locationtech.jts.geom.Point point = gf.createPoint(
                new org.locationtech.jts.geom.Coordinate(39.2084, -6.8229));
        point.setSRID(4326);
        Report report = new Report(category, "Analytics IT report", "summary input",
                point, null, owner);
        report.setCode("MTF-ANALYTICS-" + System.nanoTime());
        reportRepository.save(report);
        entityManager.flush();

        com.mtaafix.dashboard.dto.AnalyticsSummaryDto summary =
                analyticsService.summarize(java.time.Instant.EPOCH,
                        java.time.Instant.parse("9999-12-31T23:59:59Z"),
                        null, null, null, null, admin);
        assertTrue(summary.total() >= 1, "Seeded report must appear in the summary");
        assertNotNull(summary.byStatus());
        assertNotNull(summary.weekly());
        assertNotNull(summary.organisations());

        com.mtaafix.dashboard.dto.AnalyticsSummaryDto bboxSummary =
                analyticsService.summarize(null, null, null, null, null,
                        "39.0,-7.0,40.0,-6.0", admin);
        assertTrue(bboxSummary.total() >= 1,
                "bbox filter over Dar es Salaam must include the seeded report");

        com.mtaafix.dashboard.dto.AnalyticsSummaryDto outsideBox =
                analyticsService.summarize(null, null, null, null, null,
                        "10.0,10.0,11.0,11.0", admin);
        assertEquals(0L, outsideBox.total(),
                "bbox filter outside Dar es Salaam must exclude the seeded report");
    }

    @Test
    @SuppressWarnings("unchecked")
    void spatialBboxAndNearQueriesFilterByLocation() {
        User user = new User();
        user.setEmail("spatial-it-" + System.nanoTime() + "@example.com");
        user.setPasswordHash("hash");
        user.setFullName("Spatial IT User");
        user.setRole(User.Role.CITIZEN);
        userRepository.save(user);

        com.mtaafix.report.domain.ReportCategory category = entityManager.find(
                com.mtaafix.report.domain.ReportCategory.class,
                "5f0a0001-0000-4000-8000-000000000001");
        assertNotNull(category);

        org.locationtech.jts.geom.GeometryFactory gf =
                new org.locationtech.jts.geom.GeometryFactory();
        String suffix = System.nanoTime() + "";

        // Dar es Salaam area report
        org.locationtech.jts.geom.Point dar = gf.createPoint(
                new org.locationtech.jts.geom.Coordinate(39.2084, -6.8229));
        dar.setSRID(4326);
        Report inDar = new Report(category, "Dar report " + suffix, "in dar", dar, null, user);
        inDar.setCode("MTF-SPATIAL-DAR-" + suffix);
        reportRepository.save(inDar);

        // Arusha area report (~600 km away)
        org.locationtech.jts.geom.Point arusha = gf.createPoint(
                new org.locationtech.jts.geom.Coordinate(36.6833, -3.3833));
        arusha.setSRID(4326);
        Report inArusha = new Report(category, "Arusha report " + suffix, "in arusha",
                arusha, null, user);
        inArusha.setCode("MTF-SPATIAL-ARUSHA-" + suffix);
        reportRepository.save(inArusha);
        entityManager.flush();

        java.time.Instant from = java.time.Instant.EPOCH;
        java.time.Instant to = java.time.Instant.parse("9999-12-31T23:59:59Z");
        assertNotNull(from);
        assertNotNull(to);

        // Call through the service, not the repository. The live controller always
        // supplies Sort.by("reportedAt"); the service strips that sort before the
        // native query runs. Calling the repository directly with a sorted Pageable
        // bypasses that strip and Hibernate appends `order by r.reportedAt`, which
        // Postgres rejects against the native alias.
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, 50,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC,
                                "reportedAt"));

        // bbox around Dar only
        com.mtaafix.common.response.PagedResponse<
                com.mtaafix.report.dto.PublicReportSummaryDto> bboxPage =
                publicReportService.list(null, null, null, null, null,
                        "39.0,-7.0,40.0,-6.0", null, pageable);
        assertTrue(bboxPage.content().stream().anyMatch(r -> inDar.getCode().equals(r.code())),
                "bbox query must include the Dar report");
        assertTrue(bboxPage.content().stream().noneMatch(r -> inArusha.getCode().equals(r.code())),
                "bbox query must exclude the Arusha report");

        // near: 25 km around Dar
        com.mtaafix.common.response.PagedResponse<
                com.mtaafix.report.dto.PublicReportSummaryDto> nearPage =
                publicReportService.list(null, null, null, null, null,
                        null, "-6.8229,39.2084,25", pageable);
        assertTrue(nearPage.content().stream().anyMatch(r -> inDar.getCode().equals(r.code())),
                "near query must include the Dar report");
        assertTrue(nearPage.content().stream().noneMatch(r -> inArusha.getCode().equals(r.code())),
                "near query must exclude the Arusha report");

        // near: tiny radius in Arusha finds only the Arusha report
        com.mtaafix.common.response.PagedResponse<
                com.mtaafix.report.dto.PublicReportSummaryDto> arushaNear =
                publicReportService.list(null, null, null, null, null,
                        null, "-3.3833,36.6833,5", pageable);
        assertTrue(arushaNear.content().stream().anyMatch(r -> inArusha.getCode().equals(r.code())),
                "near query must include the Arusha report");
        assertTrue(arushaNear.content().stream().noneMatch(r -> inDar.getCode().equals(r.code())),
                "near query must exclude the Dar report");
    }

    @Test
    void phase14IdempotencyTableRoundTrip() {
        User user = new User();
        user.setEmail("idem-it-" + System.nanoTime() + "@example.com");
        user.setPasswordHash("hash");
        user.setFullName("Idem IT User");
        user.setRole(User.Role.CITIZEN);
        userRepository.save(user);

        com.mtaafix.report.domain.IdempotencyKey row =
                new com.mtaafix.report.domain.IdempotencyKey(
                        user, "POST /reports", "it-key-" + System.nanoTime(), "hash-value");
        idempotencyKeyRepository.save(row);
        entityManager.flush();
        entityManager.clear();

        Object nativeCount = entityManager.createNativeQuery(
                        "SELECT count(*) FROM idempotency_keys WHERE idem_key = ?1")
                .setParameter(1, row.getIdemKey())
                .getSingleResult();
        assertEquals(1, ((Number) nativeCount).intValue(),
                "row must exist in the table after save+flush");

        com.mtaafix.report.domain.IdempotencyKey found = idempotencyKeyRepository.findActive(
                user.getId(), "POST /reports", row.getIdemKey(),
                java.time.Instant.now().minus(
                        com.mtaafix.report.domain.IdempotencyKey.TTL_HOURS,
                        java.time.temporal.ChronoUnit.HOURS));
        assertNotNull(found, "V6 idempotency row must be findable while unexpired");
        assertEquals("hash-value", found.getRequestHash());

        Object uniqueIndexes = entityManager.createNativeQuery(
                        "SELECT count(*) FROM pg_indexes WHERE tablename = 'idempotency_keys' "
                                + "AND indexname = 'uq_idempotency_user_endpoint_key'")
                .getSingleResult();
        assertEquals(1, ((Number) uniqueIndexes).intValue(),
                "V6 must create the unique (user, endpoint, key) index");
    }
}
