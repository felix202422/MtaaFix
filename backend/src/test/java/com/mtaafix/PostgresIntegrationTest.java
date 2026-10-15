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
}
