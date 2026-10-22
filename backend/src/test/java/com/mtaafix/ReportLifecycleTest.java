package com.mtaafix;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtaafix.audit.repository.AuditRepository;
import com.mtaafix.report.domain.Report;
import com.mtaafix.report.repository.ReportCategoryRepository;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = MtaaFixApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportLifecycleTest {

    private static final String PASSWORD = "password123";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportCategoryRepository categoryRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private <T> T readInTx(java.util.function.Supplier<T> supplier) {
        return new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                .execute(status -> supplier.get());
    }

    private User seedUser(User.Role role) {
        User user = new User("lc-" + System.nanoTime() + "@mtaafix.test",
                BCrypt.hashpw(PASSWORD, BCrypt.gensalt()), "Lifecycle User", "+255722222222", role);
        return userRepository.save(user);
    }

    private String seedCategory() {
        String suffix = System.nanoTime() + "";
        return categoryRepository.save(new com.mtaafix.report.domain.ReportCategory(
                "Lifecycle category " + suffix, "lc-cat-" + suffix,
                com.mtaafix.report.domain.ReportCategory.Type.PRIMARY)).getId();
    }

    private String login(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail()
                                + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return MAPPER.readTree(result.getResponse().getContentAsString()).get("accessToken")
                .asText();
    }

    private String bearer(User user) throws Exception {
        return "Bearer " + login(user);
    }

    private String createReport(String token, String categoryId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + categoryId
                                + "\",\"title\":\"Lifecycle report\",\"description\":\"desc\","
                                + "\"location\":{\"latitude\":-6.8229,\"longitude\":39.2084,\"srid\":4326}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn();
        return MAPPER.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void act(String token, String reportId, String action, String body) throws Exception {
        mockMvc.perform(post("/api/v1/reports/" + reportId + "/" + action)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body == null ? "{}" : body))
                .andExpect(status().isOk());
    }

    private void assertLatestHistoryMatchesStatus(String token, String reportId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/reports/" + reportId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = MAPPER.readTree(result.getResponse().getContentAsString());
        String statusText = node.get("status").asText();
        JsonNode history = node.get("statusHistory");
        JsonNode latest = history.get(history.size() - 1);
        org.junit.jupiter.api.Assertions.assertEquals(statusText, latest.get("toStatus").asText(),
                "Invariant: latest history to_status must equal current status");
    }

    @Test
    void fullLifecycleJourneyImplementsTransitionTable() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);
        User worker = seedUser(User.Role.FIELD_WORKER);
        String cToken = bearer(citizen);
        String mToken = bearer(moderator);
        String aToken = bearer(admin);
        String wToken = bearer(worker);
        String categoryId = seedCategory();

        String id = createReport(cToken, categoryId);
        assertLatestHistoryMatchesStatus(cToken, id);

        act(mToken, id, "submit", null);
        assertLatestHistoryMatchesStatus(cToken, id);
        act(mToken, id, "needs-info", "{\"message\":\"Please add a clearer photo\"}");
        act(cToken, id, "resubmit", null);
        act(mToken, id, "verify", null);
        mockMvc.perform(post("/api/v1/reports/" + id + "/assign")
                        .header("Authorization", aToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + worker.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
        act(wToken, id, "start", null);
        mockMvc.perform(post("/api/v1/reports/" + id + "/complete")
                        .header("Authorization", wToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completionNote\":\"Fixed\"}"))
                .andExpect(status().isBadRequest());
        act(wToken, id, "media",
                "{\"fileName\":\"p.jpg\",\"originalName\":\"p.jpg\",\"contentType\":\"image/jpeg\","
                        + "\"sizeBytes\":2048,"
                        + "\"url\":\"/api/v1/media/11111111-2222-3333-4444-555555555555/content\","
                        + "\"mediaType\":\"PHOTO\"}");
        act(wToken, id, "complete", "{\"completionNote\":\"Fixed\"}");
        act(mToken, id, "return", "{\"comment\":\"Pothole still open\"}");
        act(wToken, id, "complete", "{\"completionNote\":\"Fixed again\"}");
        act(mToken, id, "resolve", null);
        act(cToken, id, "dispute", "{\"disputeReason\":\"Still flooding\"}");
        act(wToken, id, "complete", "{\"completionNote\":\"Done for real\"}");
        act(mToken, id, "resolve", null);
        act(cToken, id, "confirm", "{\"feedback\":\"Thanks\"}");
        act(aToken, id, "reopen", "{\"justification\":\"Recurring issue in same location\"}");
        act(mToken, id, "submit", null);
        act(mToken, id, "verify", null);

        assertLatestHistoryMatchesStatus(cToken, id);
        readInTx(() -> {
            Report report = reportRepository.findById(id).orElseThrow();
            org.junit.jupiter.api.Assertions.assertEquals(Report.Status.VERIFIED, report.getStatus());
            long transitions = report.getStatusHistory().stream()
                    .filter(h -> h.getFromStatus() != null).count();
            org.junit.jupiter.api.Assertions.assertTrue(transitions >= 16,
                    "Every transition must append a history row (FR-3.7), got " + transitions);
            report.getStatusHistory().forEach(h -> org.junit.jupiter.api.Assertions
                    .assertNotNull(h.getUser(), "Every transition history row must record an actor"));
            return null;
        });
    }

    @Test
    void forbiddenStateTransitionsReturn409() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);
        User worker = seedUser(User.Role.FIELD_WORKER);
        String cToken = bearer(citizen);
        String mToken = bearer(moderator);
        String aToken = bearer(admin);
        String wToken = bearer(worker);
        String categoryId = seedCategory();

        String submitted = createReport(cToken, categoryId);
        mockMvc.perform(post("/api/v1/reports/" + submitted + "/resolve")
                        .header("Authorization", mToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        String verified = createReport(cToken, categoryId);
        act(mToken, verified, "submit", null);
        act(mToken, verified, "verify", null);
        mockMvc.perform(post("/api/v1/reports/" + verified + "/duplicate")
                        .header("Authorization", mToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canonicalReportRef\":\"" + submitted + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        mockMvc.perform(post("/api/v1/reports/" + verified + "/confirm")
                        .header("Authorization", cToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        mockMvc.perform(post("/api/v1/reports/" + verified + "/reopen")
                        .header("Authorization", aToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justification\":\"should fail\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        String assigned = createReport(cToken, categoryId);
        act(mToken, assigned, "submit", null);
        act(mToken, assigned, "verify", null);
        mockMvc.perform(post("/api/v1/reports/" + assigned + "/assign")
                        .header("Authorization", aToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + worker.getId() + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reports/" + assigned + "/resolve")
                        .header("Authorization", mToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        mockMvc.perform(post("/api/v1/reports/" + assigned + "/complete")
                        .header("Authorization", wToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void roleGatesOnLifecycleEndpointsFollowMatrix() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User stranger = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);
        User worker = seedUser(User.Role.FIELD_WORKER);
        String cToken = bearer(citizen);
        String sToken = bearer(stranger);
        String mToken = bearer(moderator);
        String aToken = bearer(admin);
        String wToken = bearer(worker);
        String categoryId = seedCategory();

        String id = createReport(cToken, categoryId);

        mockMvc.perform(post("/api/v1/reports/" + id + "/needs-info")
                        .header("Authorization", cToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + id + "/duplicate")
                        .header("Authorization", cToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canonicalReportRef\":\"x\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + id + "/start")
                        .header("Authorization", mToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + id + "/complete")
                        .header("Authorization", aToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + id + "/return")
                        .header("Authorization", cToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"x\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + id + "/reopen")
                        .header("Authorization", mToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justification\":\"x\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + id + "/resubmit")
                        .header("Authorization", mToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + id + "/confirm")
                        .header("Authorization", sToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + id + "/dispute")
                        .header("Authorization", wToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disputeReason\":\"x\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + id + "/comments")
                        .header("Authorization", sToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + id + "/media")
                        .header("Authorization", sToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"a\",\"originalName\":\"a\",\"contentType\":\"image/jpeg\","
                                + "\"sizeBytes\":100,\"url\":\"u\",\"mediaType\":\"PHOTO\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + id + "/close")
                        .header("Authorization", mToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void requiredFieldsAndValidationFailWith400() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);
        User worker = seedUser(User.Role.FIELD_WORKER);
        String cToken = bearer(citizen);
        String mToken = bearer(moderator);
        String aToken = bearer(admin);
        String wToken = bearer(worker);
        String categoryId = seedCategory();

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", cToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"no category\",\"location\":{\"latitude\":-6.8,\"longitude\":39.2}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", cToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"does-not-exist\",\"title\":\"x\","
                                + "\"location\":{\"latitude\":-6.8,\"longitude\":39.2}}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", cToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + categoryId + "\",\"title\":\"bad coords\","
                                + "\"location\":{\"latitude\":91.0,\"longitude\":39.2}}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", cToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + categoryId + "\",\"title\":\"bad coords\","
                                + "\"location\":{\"latitude\":-6.8,\"longitude\":-181.0}}"))
                .andExpect(status().isBadRequest());

        String id = createReport(cToken, categoryId);
        act(mToken, id, "submit", null);

        mockMvc.perform(post("/api/v1/reports/" + id + "/needs-info")
                        .header("Authorization", mToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/reports/" + id + "/reject")
                        .header("Authorization", mToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"missing code\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/reports/" + id + "/duplicate")
                        .header("Authorization", mToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        String verifiedId = createReport(cToken, categoryId);
        act(mToken, verifiedId, "submit", null);
        act(mToken, verifiedId, "verify", null);
        mockMvc.perform(post("/api/v1/reports/" + verifiedId + "/assign")
                        .header("Authorization", aToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + worker.getId() + "\"}"))
                .andExpect(status().isOk());
        act(wToken, verifiedId, "start", null);
        mockMvc.perform(post("/api/v1/reports/" + verifiedId + "/complete")
                        .header("Authorization", wToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        mockMvc.perform(post("/api/v1/reports/" + verifiedId + "/return")
                        .header("Authorization", mToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/reports/" + verifiedId + "/reopen")
                        .header("Authorization", aToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/reports/" + verifiedId + "/media")
                        .header("Authorization", wToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"a\",\"originalName\":\"a\","
                                + "\"contentType\":\"application/x-msdownload\",\"sizeBytes\":100,"
                                + "\"url\":\"u\",\"mediaType\":\"PHOTO\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/reports/" + verifiedId + "/media")
                        .header("Authorization", wToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"a\",\"originalName\":\"a\","
                                + "\"contentType\":\"image/jpeg\",\"sizeBytes\":999999999,"
                                + "\"url\":\"u\",\"mediaType\":\"PHOTO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void codesAreUniqueFormattedAndStable() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        String token = bearer(citizen);
        String categoryId = seedCategory();

        MvcResult first = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + categoryId + "\",\"title\":\"code one\","
                                + "\"location\":{\"latitude\":-6.8,\"longitude\":39.2}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(matchesPattern("MTF-\\d{4}-\\d{6}")))
                .andReturn();
        MvcResult second = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + categoryId + "\",\"title\":\"code two\","
                                + "\"location\":{\"latitude\":-6.8,\"longitude\":39.2}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(matchesPattern("MTF-\\d{4}-\\d{6}")))
                .andReturn();
        String codeOne = MAPPER.readTree(first.getResponse().getContentAsString()).get("code")
                .asText();
        String codeTwo = MAPPER.readTree(second.getResponse().getContentAsString()).get("code")
                .asText();
        org.junit.jupiter.api.Assertions.assertNotEquals(codeOne, codeTwo);

        String id = MAPPER.readTree(first.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(get("/api/v1/reports/" + id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(codeOne));
    }

    @Test
    void listIsRoleScopedAndFilterable() throws Exception {
        User citizenA = seedUser(User.Role.CITIZEN);
        User citizenB = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        String aToken = bearer(citizenA);
        String bToken = bearer(citizenB);
        String mToken = bearer(moderator);
        String categoryId = seedCategory();

        String a1 = createReport(aToken, categoryId);
        String a2 = createReport(aToken, categoryId);
        String b1 = createReport(bToken, categoryId);
        org.junit.jupiter.api.Assertions.assertNotEquals(a1, b1);

        MvcResult own = mockMvc.perform(get("/api/v1/reports")
                        .header("Authorization", aToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andReturn();
        JsonNode ownNode = MAPPER.readTree(own.getResponse().getContentAsString());
        List<String> ownIds = new ArrayList<>();
        ownNode.get("content").forEach(n -> ownIds.add(n.get("id").asText()));
        org.junit.jupiter.api.Assertions.assertTrue(ownIds.contains(a1));
        org.junit.jupiter.api.Assertions.assertTrue(ownIds.contains(a2));
        org.junit.jupiter.api.Assertions.assertFalse(ownIds.contains(b1));

        act(mToken, a1, "submit", null);
        mockMvc.perform(get("/api/v1/reports?status=SUBMITTED")
                        .header("Authorization", aToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(a2));
        mockMvc.perform(get("/api/v1/reports?status=UNDER_REVIEW")
                        .header("Authorization", aToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(a1));

        mockMvc.perform(get("/api/v1/reports?status=NOT_A_STATUS")
                        .header("Authorization", aToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/reports?categoryId=" + categoryId)
                        .header("Authorization", mToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    void duplicateMarkingLinksCanonicalAndNotifies() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        String cToken = bearer(citizen);
        String mToken = bearer(moderator);
        String categoryId = seedCategory();

        String canonicalId = createReport(cToken, categoryId);
        String duplicateId = createReport(cToken, categoryId);
        String canonicalCode = reportRepository.findById(canonicalId).orElseThrow().getCode();

        act(mToken, canonicalId, "submit", null);
        act(mToken, canonicalId, "verify", null);
        act(mToken, duplicateId, "submit", null);

        mockMvc.perform(post("/api/v1/reports/" + duplicateId + "/duplicate")
                        .header("Authorization", mToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canonicalReportRef\":\"" + canonicalCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"))
                .andExpect(jsonPath("$.duplicateOfId").value(canonicalId));

        readInTx(() -> {
            Report duplicate = reportRepository.findById(duplicateId).orElseThrow();
            org.junit.jupiter.api.Assertions.assertEquals(canonicalId,
                    duplicate.getDuplicateOf().getId());
            Report canonical = reportRepository.findById(canonicalId).orElseThrow();
            org.junit.jupiter.api.Assertions.assertTrue(canonical.getStatusHistory().stream()
                    .anyMatch(h -> "DUPLICATE_LINK".equals(h.getReasonCode())),
                    "Canonical report history must note the duplicate link (invariant 7)");
            return null;
        });
        assertLatestHistoryMatchesStatus(cToken, duplicateId);
    }

    @Test
    void privilegedTransitionsWriteAuditEntries() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);
        User worker = seedUser(User.Role.FIELD_WORKER);
        String cToken = bearer(citizen);
        String mToken = bearer(moderator);
        String aToken = bearer(admin);
        String wToken = bearer(worker);
        String categoryId = seedCategory();

        String id = createReport(cToken, categoryId);
        act(mToken, id, "submit", null);
        act(mToken, id, "verify", null);
        mockMvc.perform(post("/api/v1/reports/" + id + "/assign")
                        .header("Authorization", aToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + worker.getId() + "\"}"))
                .andExpect(status().isOk());
        act(wToken, id, "start", null);
        act(wToken, id, "media",
                "{\"fileName\":\"e.jpg\",\"originalName\":\"e.jpg\",\"contentType\":\"image/jpeg\","
                        + "\"sizeBytes\":1024,"
                        + "\"url\":\"/api/v1/media/66666666-7777-8888-9999-000000000000/content\","
                        + "\"mediaType\":\"PHOTO\"}");
        act(wToken, id, "complete", "{}");
        act(mToken, id, "resolve", null);

        List<String> actions = auditRepository.findByEntityId(id).stream()
                .map(entry -> entry.getAction()).toList();
        org.junit.jupiter.api.Assertions.assertTrue(actions.contains("REPORT_VERIFIED"),
                "verify must audit, got " + actions);
        org.junit.jupiter.api.Assertions.assertTrue(actions.contains("REPORT_ASSIGNED"),
                "assign must audit, got " + actions);
        org.junit.jupiter.api.Assertions.assertTrue(actions.contains("REPORT_RESOLVED"),
                "resolve must audit, got " + actions);
    }

    @Test
    void commentsAndMediaAttachToReports() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        String cToken = bearer(citizen);
        String mToken = bearer(moderator);
        String categoryId = seedCategory();

        String id = createReport(cToken, categoryId);

        act(cToken, id, "comments", "{\"content\":\"Additional detail from citizen\"}");
        mockMvc.perform(get("/api/v1/reports/" + id).header("Authorization", cToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments", hasSize(1)))
                .andExpect(jsonPath("$.comments[0].content")
                        .value("Additional detail from citizen"));

        mockMvc.perform(post("/api/v1/reports/" + id + "/media")
                        .header("Authorization", mToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"proof.jpg\",\"originalName\":\"proof.jpg\","
                                + "\"contentType\":\"image/png\",\"sizeBytes\":4096,"
                                + "\"url\":\"/api/v1/media/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/content\","
                                + "\"mediaType\":\"PHOTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.media", hasSize(1)));

        String mediaId = readInTx(() -> reportRepository.findById(id).orElseThrow()
                .getMedia().get(0).getId());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/media/" + mediaId)
                        .header("Authorization", mToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/reports/" + id).header("Authorization", cToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.media", hasSize(0)));
    }

    @Test
    void editingOnlyAllowedBeforeAssignment() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        String cToken = bearer(citizen);
        String mToken = bearer(moderator);
        String categoryId = seedCategory();

        String id = createReport(cToken, categoryId);
        mockMvc.perform(put("/api/v1/reports/" + id)
                        .header("Authorization", cToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Edited before review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Edited before review"));

        act(mToken, id, "submit", null);
        act(mToken, id, "needs-info", "{\"message\":\"clarify\"}");
        mockMvc.perform(put("/api/v1/reports/" + id)
                        .header("Authorization", cToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Edited while needing info\"}"))
                .andExpect(status().isOk());

        act(cToken, id, "resubmit", null);
        mockMvc.perform(put("/api/v1/reports/" + id)
                        .header("Authorization", cToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"too late\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void workerOnlySeesAndActsOnTheirOwnAssignments() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User workerA = seedUser(User.Role.FIELD_WORKER);
        User workerB = seedUser(User.Role.FIELD_WORKER);
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);
        String cToken = bearer(citizen);
        String mToken = bearer(moderator);
        String aToken = bearer(admin);
        String w1Token = bearer(workerA);
        String w2Token = bearer(workerB);
        String categoryId = seedCategory();

        String id = createReport(cToken, categoryId);
        act(mToken, id, "submit", null);
        act(mToken, id, "verify", null);
        mockMvc.perform(post("/api/v1/reports/" + id + "/assign")
                        .header("Authorization", aToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + workerA.getId() + "\"}"))
                .andExpect(status().isOk());

        // Worker B has no visibility into or control over worker A's assignment.
        mockMvc.perform(get("/api/v1/reports/" + id)
                        .header("Authorization", w2Token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + id + "/start")
                        .header("Authorization", w2Token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + id + "/complete")
                        .header("Authorization", w2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completionNote\":\"not mine\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + id + "/comments")
                        .header("Authorization", w2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"sneaky note\"}"))
                .andExpect(status().isForbidden());

        // Assignment lists are strictly self-scoped.
        mockMvc.perform(get("/api/v1/assignments/me")
                        .header("Authorization", w1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
        mockMvc.perform(get("/api/v1/assignments/me")
                        .header("Authorization", w2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        // Worker A retains full permitted access to their own assignment.
        mockMvc.perform(get("/api/v1/reports/" + id)
                        .header("Authorization", w1Token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reports/" + id + "/start")
                        .header("Authorization", w1Token))
                .andExpect(status().isOk());
    }
}
