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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = MtaaFixApplication.class)
@EnabledIfEnvironmentVariable(named = "MTAAFIX_PG_IT", matches = "true")
@Transactional
class PostgresIntegrationTest {

    @Autowired
    private ReportRepository reportRepository;

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

        Report report = new Report(Report.Subject.POTHOLE, "IT pothole", "round trip",
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
}
