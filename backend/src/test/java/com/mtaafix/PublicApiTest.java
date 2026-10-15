package com.mtaafix;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mtaafix.report.domain.Report;
import com.mtaafix.report.domain.ReportCategory;
import com.mtaafix.report.domain.ReportComment;
import com.mtaafix.report.domain.ReportMedia;
import com.mtaafix.report.domain.ReportStatusHistory;
import com.mtaafix.report.repository.ReportCategoryRepository;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = MtaaFixApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicApiTest {

    private static final String PASSWORD = "password123";
    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportCategoryRepository categoryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private <T> T readInTx(java.util.function.Supplier<T> supplier) {
        return new TransactionTemplate(transactionManager).execute(status -> supplier.get());
    }

    private ReportCategory seedCategory() {
        String suffix = System.nanoTime() + "";
        return categoryRepository.save(
                new ReportCategory("Public cat " + suffix, "pub-cat-" + suffix,
                        ReportCategory.Type.PRIMARY));
    }

    private User seedUser() {
        return userRepository.save(new User("pub-" + System.nanoTime() + "@mtaafix.test",
                BCrypt.hashpw(PASSWORD, BCrypt.gensalt()), "Jane Citizen Reporter",
                "+255713999888", User.Role.CITIZEN));
    }

    private Report seedFullDataReport(ReportCategory category, User reporter, String title,
            Report.Status status) {
        Report report = new Report(category, title,
                "Pothole near the main road junction requiring urgent repair.",
                GF.createPoint(new Coordinate(39.20841234, -6.82299876)), null, reporter);
        report.setCode("MTF-2026-" + String.format("%06d", Math.abs(title.hashCode() % 1000000)));
        report.setStatus(status);
        report.getComments()
                .add(new ReportComment("Internal moderation note: verify with the ward officer.",
                        report, reporter));
        report.getMedia().add(new ReportMedia("file.jpg", "street-pothole.jpg", "image/jpeg",
                1024L, "/media/file.jpg", ReportMedia.MediaType.PHOTO, report, reporter));
        report.getStatusHistory().add(new ReportStatusHistory(report, Report.Status.SUBMITTED,
                Report.Status.UNDER_REVIEW, reporter, "Claimed for review.", null));
        report.getStatusHistory().add(new ReportStatusHistory(report,
                Report.Status.UNDER_REVIEW, Report.Status.VERIFIED, reporter,
                "Confirmed with the ward office.", null));
        if (status == Report.Status.RESOLVED || status == Report.Status.CLOSED) {
            report.getStatusHistory().add(new ReportStatusHistory(report,
                    Report.Status.VERIFIED, Report.Status.ASSIGNED, reporter, null, null));
            report.getStatusHistory().add(new ReportStatusHistory(report,
                    Report.Status.ASSIGNED, Report.Status.IN_PROGRESS, reporter, null, null));
            report.getStatusHistory().add(new ReportStatusHistory(report,
                    Report.Status.IN_PROGRESS, Report.Status.RESOLUTION_REVIEW, reporter,
                    "Work completed.", null));
            report.getStatusHistory().add(new ReportStatusHistory(report,
                    Report.Status.RESOLUTION_REVIEW, Report.Status.RESOLVED, reporter,
                    "Verified fix.", null));
            if (status == Report.Status.CLOSED) {
                report.getStatusHistory().add(new ReportStatusHistory(report,
                        Report.Status.RESOLVED, Report.Status.CLOSED, reporter, null, null));
            }
        }
        return reportRepository.save(report);
    }

    @Test
    void publicListIsAccessibleWithoutAuthAndReturnsPagedShape() throws Exception {
        ReportCategory category = readInTx(() -> seedCategory());
        readInTx(() -> seedFullDataReport(category, seedUser(), "Open drain on Main Street",
                Report.Status.SUBMITTED));

        mockMvc.perform(get("/api/v1/reports/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThan(0)))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].code").isNotEmpty())
                .andExpect(jsonPath("$.content[0].title").isNotEmpty())
                .andExpect(jsonPath("$.content[0].status").isNotEmpty())
                .andExpect(jsonPath("$.content[0].categoryName").isNotEmpty());
    }

    @Test
    void publicPayloadNeverExposesIdentityCommentsMediaOrPreciseLocation() throws Exception {
        ReportCategory category = readInTx(() -> seedCategory());
        Report report = readInTx(() -> seedFullDataReport(category, seedUser(),
                "Broken streetlight at Samora Avenue", Report.Status.VERIFIED));
        String code = readInTx(() -> reportRepository.findByCode(report.getCode())
                .map(Report::getCode).orElseThrow());

        String listBody = mockMvc.perform(get("/api/v1/reports/public")
                        .param("q", "Samora"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value(code))
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertFalse(
                        listBody.contains("Jane Citizen Reporter"),
                        "public list must not contain reporter name"),
                () -> org.junit.jupiter.api.Assertions.assertFalse(
                        listBody.contains("pub-") && listBody.contains("@mtaafix.test"),
                        "public list must not contain reporter email"),
                () -> org.junit.jupiter.api.Assertions.assertFalse(
                        listBody.contains("+255713999888"),
                        "public list must not contain reporter phone"),
                () -> org.junit.jupiter.api.Assertions.assertFalse(
                        listBody.contains("Internal moderation note"),
                        "public list must not contain comments"),
                () -> org.junit.jupiter.api.Assertions.assertFalse(
                        listBody.contains("street-pothole.jpg"),
                        "public list must not contain media"),
                () -> org.junit.jupiter.api.Assertions.assertFalse(
                        listBody.contains("6.8229") || listBody.contains("39.20841"),
                        "public list must not contain precise coordinates"));

        String detailBody = mockMvc.perform(get("/api/v1/reports/public/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.latitude").isNumber())
                .andExpect(jsonPath("$.longitude").isNumber())
                .andExpect(jsonPath("$.geoJson.type").value("Point"))
                .andExpect(jsonPath("$.geoJson.coordinates").isArray())
                .andExpect(jsonPath("$.geoJson.coordinates.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertFalse(
                        detailBody.contains("Jane Citizen Reporter"),
                        "detail must not contain reporter name"),
                () -> org.junit.jupiter.api.Assertions.assertFalse(
                        detailBody.contains("+255713999888"),
                        "detail must not contain reporter phone"),
                () -> org.junit.jupiter.api.Assertions.assertFalse(
                        detailBody.contains("Internal moderation note"),
                        "detail must not contain internal comments"),
                () -> org.junit.jupiter.api.Assertions.assertFalse(
                        detailBody.contains("Verify with the ward officer"),
                        "detail must not contain moderation notes"),
                () -> org.junit.jupiter.api.Assertions.assertFalse(
                        detailBody.contains("media"),
                        "detail must not serialize media (deferred Phase 10/13)"),
                () -> org.junit.jupiter.api.Assertions.assertFalse(
                        detailBody.contains("userId"),
                        "detail must not expose user ids"),
                () -> org.junit.jupiter.api.Assertions.assertFalse(
                        detailBody.contains("6.8229") || listBody.contains("39.20841"),
                        "detail must not contain precise coordinates"));
    }

    @Test
    void publicLocationIsFuzzedDeterministically() throws Exception {
        ReportCategory category = readInTx(() -> seedCategory());
        Report report = readInTx(() -> seedFullDataReport(category, seedUser(),
                "Deterministic fuzz check", Report.Status.SUBMITTED));
        String code = report.getCode();

        String first = mockMvc.perform(get("/api/v1/reports/public/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").isNumber())
                .andExpect(jsonPath("$.longitude").isNumber())
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(get("/api/v1/reports/public/" + code))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode a = mapper.readTree(first);
        com.fasterxml.jackson.databind.JsonNode b = mapper.readTree(second);
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertEquals(
                        a.get("latitude").asDouble(), b.get("latitude").asDouble(), 0.0,
                        "fuzzed latitude must be stable across requests"),
                () -> org.junit.jupiter.api.Assertions.assertEquals(
                        a.get("longitude").asDouble(), b.get("longitude").asDouble(), 0.0,
                        "fuzzed longitude must be stable across requests"),
                () -> org.junit.jupiter.api.Assertions.assertNotEquals(-6.82299876,
                        a.get("latitude").asDouble(), 1e-6,
                        "fuzzed latitude must differ from the precise value"),
                () -> org.junit.jupiter.api.Assertions.assertNotEquals(39.20841234,
                        a.get("longitude").asDouble(), 1e-6,
                        "fuzzed longitude must differ from the precise value"));
    }

    @Test
    void publicDetailExposesHighLevelTimelineOnly() throws Exception {
        ReportCategory category = readInTx(() -> seedCategory());
        Report report = readInTx(() -> seedFullDataReport(category, seedUser(),
                "Waste collection missed in Mikocheni", Report.Status.RESOLVED));
        String code = report.getCode();

        mockMvc.perform(get("/api/v1/reports/public/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty())
                .andExpect(jsonPath("$.closedAt").value(nullValue()))
                .andExpect(jsonPath("$.timeline", hasSize(greaterThan(1))))
                .andExpect(jsonPath("$.timeline[0].status").isNotEmpty())
                .andExpect(jsonPath("$.timeline[0].timestamp").isNotEmpty())
                .andExpect(jsonPath("$.timeline[0].comment").doesNotExist())
                .andExpect(jsonPath("$.timeline[0].reasonCode").doesNotExist())
                .andExpect(jsonPath("$.timeline[0].userId").doesNotExist());
    }

    @Test
    void publicListSupportsSearchStatusCategoryAndDateFilters() throws Exception {
        ReportCategory categoryA = readInTx(() -> seedCategory());
        ReportCategory categoryB = readInTx(() -> seedCategory());
        User user = readInTx(() -> seedUser());
        readInTx(() -> seedFullDataReport(categoryA, user, "Alpha flooding near river",
                Report.Status.SUBMITTED));
        readInTx(() -> seedFullDataReport(categoryB, seedUser(), "Beta broken sidewalk",
                Report.Status.VERIFIED));

        mockMvc.perform(get("/api/v1/reports/public").param("q", "flooding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title", containsString("flooding")));

        mockMvc.perform(get("/api/v1/reports/public")
                        .param("status", "VERIFIED").param("categoryId", categoryB.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("VERIFIED"))
                .andExpect(jsonPath("$.content[0].categoryId").value(categoryB.getId()));

        mockMvc.perform(get("/api/v1/reports/public").param("categoryId", categoryB.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].categoryId").value(categoryB.getId()));

        mockMvc.perform(get("/api/v1/reports/public")
                        .param("dateFrom", "2000-01-01").param("dateTo", "2001-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/v1/reports/public")
                        .param("dateFrom",
                                java.time.LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1)
                                        .toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThan(0)));
    }

    @Test
    void publicListRejectsInvalidFilters() throws Exception {
        mockMvc.perform(get("/api/v1/reports/public").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/v1/reports/public").param("dateFrom", "yesterday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/v1/reports/public")
                        .param("dateFrom", "2026-10-10").param("dateTo", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void publicListRejectsInvalidGeoFilters() throws Exception {
        // Malformed / out-of-range / reversed bbox values (parsing is cross-dialect;
        // happy-path spatial execution is covered by PostgreSQL IT).
        mockMvc.perform(get("/api/v1/reports/public").param("bbox", "1,2,3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/v1/reports/public").param("bbox", "a,b,c,d"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/v1/reports/public")
                        .param("bbox", "40,-7,39,-6"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/v1/reports/public")
                        .param("bbox", "200,-10,210,-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/v1/reports/public").param("near", "5,30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/v1/reports/public")
                        .param("near", "-6.8,39.2,-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/v1/reports/public")
                        .param("near", "95,39.2,5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // bbox and near are mutually exclusive
        mockMvc.perform(get("/api/v1/reports/public")
                        .param("bbox", "39,-7,40,-6").param("near", "-6.8,39.2,5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void publicDetailReturns404ForUnknownCode() throws Exception {
        mockMvc.perform(get("/api/v1/reports/public/MTF-2099-999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void categoriesPublicIsAccessibleWithoutAuth() throws Exception {
        readInTx(() -> seedCategory());
        mockMvc.perform(get("/api/v1/categories/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].name").isNotEmpty())
                .andExpect(jsonPath("$[0].slug").isNotEmpty());
    }

    @Test
    void statsPublicReturnsAggregateCountsWithoutAuth() throws Exception {
        ReportCategory category = readInTx(() -> seedCategory());
        readInTx(() -> seedFullDataReport(category, seedUser(), "Stats submitted report",
                Report.Status.SUBMITTED));
        readInTx(() -> seedFullDataReport(category, seedUser(), "Stats resolved report",
                Report.Status.RESOLVED));

        mockMvc.perform(get("/api/v1/stats/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").value(greaterThan(1)))
                .andExpect(jsonPath("$.submittedReports").value(greaterThan(0)))
                .andExpect(jsonPath("$.resolvedReports").value(greaterThan(0)))
                .andExpect(jsonPath("$.categories").value(greaterThan(0)));
    }

    @Test
    void authenticatedReportEndpointsStillRequireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/reports"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateReportExposesCanonicalCodeOnPublicDetail() throws Exception {
        ReportCategory category = readInTx(() -> seedCategory());
        User user = readInTx(() -> seedUser());
        Report canonical = readInTx(() -> seedFullDataReport(category, user,
                "Canonical pothole report", Report.Status.VERIFIED));
        Report duplicate = readInTx(() -> {
            Report d = seedFullDataReport(category, seedUser(),
                    "Duplicate pothole report nearby", Report.Status.DUPLICATE);
            d.setDuplicateOf(canonical);
            return reportRepository.save(d);
        });

        mockMvc.perform(get("/api/v1/reports/public/" + duplicate.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"))
                .andExpect(jsonPath("$.duplicateOfCode").value(canonical.getCode()));
    }
}
