package com.mtaafix;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtaafix.notification.domain.NotificationDelivery;
import com.mtaafix.notification.repository.NotificationDeliveryRepository;
import com.mtaafix.report.repository.ReportCategoryRepository;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Phase 13: preferences (FR-6.4), delivery separation (FR-6.3), unread badge, event fixes. */
@SpringBootTest(classes = MtaaFixApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationPhase13Test {

    private static final String PASSWORD = "password123";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReportCategoryRepository categoryRepository;
    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    private User seedUser(User.Role role) {
        return userRepository.save(new User("n13-" + System.nanoTime() + "@mtaafix.test",
                BCrypt.hashpw(PASSWORD, BCrypt.gensalt()), "Phase Thirteen", "+255744444444",
                role));
    }

    private String seedCategory() {
        String suffix = System.nanoTime() + "";
        return categoryRepository.save(new com.mtaafix.report.domain.ReportCategory(
                "Phase13 category " + suffix, "n13-cat-" + suffix,
                com.mtaafix.report.domain.ReportCategory.Type.PRIMARY)).getId();
    }

    private String bearer(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail()
                                + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + MAPPER.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String createReport(String token, String categoryId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + categoryId + "\",\"title\":\"" + title
                                + "\",\"description\":\"desc\","
                                + "\"location\":{\"latitude\":-6.82,\"longitude\":39.21,\"srid\":4326}}"))
                .andExpect(status().isCreated())
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

    private String latestNotificationId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        return MAPPER.readTree(result.getResponse().getContentAsString())
                .get("content").get(0).get("id").asText();
    }

    private Map<String, String> deliveryStatuses(String notificationId) {
        List<NotificationDelivery> rows = deliveryRepository.findByNotificationId(notificationId);
        return rows.stream()
                .collect(Collectors.toMap(d -> d.getChannel().name(),
                        d -> d.getStatus().name()));
    }

    // --- unread count ---

    @Test
    void unreadCountFollowsCreationAndReadState() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        String token = bearer(citizen);
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        createReport(token, seedCategory(), "Unread badge report");
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        String notificationId = latestNotificationId(token);
        mockMvc.perform(post("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    // --- preferences (FR-6.4) ---

    @Test
    void preferencesExposeDefaultsAndHonorPartialUpdates() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        String token = bearer(citizen);
        mockMvc.perform(get("/api/v1/notifications/preferences")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(true))
                .andExpect(jsonPath("$.pushEnabled").value(false))
                .andExpect(jsonPath("$.pushAvailable").value(false));

        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailEnabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.pushEnabled").value(false));

        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pushEnabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.pushEnabled").value(true));

        mockMvc.perform(get("/api/v1/notifications/preferences")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.pushEnabled").value(true));
    }

    @Test
    void preferencesAreScopedToTheirOwner() throws Exception {
        User alice = seedUser(User.Role.CITIZEN);
        User bob = seedUser(User.Role.CITIZEN);
        String aToken = bearer(alice);
        String bToken = bearer(bob);

        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .header("Authorization", aToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailEnabled\":false,\"pushEnabled\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications/preferences")
                        .header("Authorization", bToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(true))
                .andExpect(jsonPath("$.pushEnabled").value(false));
    }

    // --- delivery separation (FR-6.3) ---

    @Test
    void deliveriesSeparateCreationFromChannelState() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        String token = bearer(citizen);
        String reportId = createReport(token, seedCategory(), "Delivery separation");

        String notificationId = latestNotificationId(token);
        Map<String, String> statuses = deliveryStatuses(notificationId);
        org.junit.jupiter.api.Assertions.assertEquals("DELIVERED", statuses.get("IN_APP"),
                "in-app delivery must be recorded, got " + statuses);
        org.junit.jupiter.api.Assertions.assertEquals("SKIPPED", statuses.get("EMAIL"),
                "email must be SKIPPED while the channel is disabled, got " + statuses);

        // Opting out of email creates no email delivery row at all.
        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailEnabled\":false}"))
                .andExpect(status().isOk());
        act(bearer(seedUser(User.Role.MODERATOR)), reportId, "submit", null);
        String reviewNotificationId = latestNotificationId(token);
        Map<String, String> afterOptOut = deliveryStatuses(reviewNotificationId);
        org.junit.jupiter.api.Assertions.assertEquals(Set.of("IN_APP"),
                afterOptOut.keySet(), "opted-out users get in-app only, got " + afterOptOut);
    }

    // --- lifecycle event fixes ---

    @Test
    void cancelNotifiesWithDedicatedCancelledType() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        String token = bearer(citizen);
        String id = createReport(token, seedCategory(), "Cancel type");
        act(token, id, "cancel", null);

        MvcResult result = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = MAPPER.readTree(result.getResponse().getContentAsString()).get("content");
        boolean found = false;
        for (JsonNode item : content) {
            if ("REPORT_CANCELLED".equals(item.get("type").asText())) {
                found = true;
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(found,
                "cancel must notify with REPORT_CANCELLED");
    }

    @Test
    void disputeNotifiesOwnerAndAssigneeAndKeepsAccessMatrix() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);
        User worker = seedUser(User.Role.FIELD_WORKER);
        User stranger = seedUser(User.Role.CITIZEN);
        String cToken = bearer(citizen);
        String mToken = bearer(moderator);
        String aToken = bearer(admin);
        String wToken = bearer(worker);
        String sToken = bearer(stranger);
        String id = createReport(cToken, seedCategory(), "Dispute notify");

        act(mToken, id, "submit", null);
        act(mToken, id, "verify", null);
        mockMvc.perform(post("/api/v1/reports/" + id + "/assign")
                        .header("Authorization", aToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + worker.getId() + "\"}"))
                .andExpect(status().isOk());
        act(wToken, id, "start", null);

        // Real multipart evidence as the assigned worker (FR-9.1).
        byte[] jpeg = new byte[64];
        jpeg[0] = (byte) 0xFF;
        jpeg[1] = (byte) 0xD8;
        jpeg[2] = (byte) 0xFF;
        jpeg[3] = (byte) 0xE0;
        MvcResult upload = mockMvc.perform(multipart("/api/v1/reports/" + id + "/media/upload")
                        .file(new MockMultipartFile("file", "fix.jpg", "image/jpeg", jpeg))
                        .header("Authorization", wToken))
                .andExpect(status().isOk())
                .andReturn();
        String mediaId = MAPPER.readTree(upload.getResponse().getContentAsString())
                .get("media").get(0).get("id").asText();

        // Access matrix on content: assigned worker + moderator pass, stranger 403.
        mockMvc.perform(get("/api/v1/media/" + mediaId + "/content")
                        .header("Authorization", wToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/media/" + mediaId + "/content")
                        .header("Authorization", mToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/media/" + mediaId + "/content")
                        .header("Authorization", sToken))
                .andExpect(status().isForbidden());

        act(wToken, id, "complete", "{\"completionNote\":\"Fixed\"}");
        act(mToken, id, "resolve", null);
        act(cToken, id, "dispute", "{\"disputeReason\":\"Still broken\"}");

        assertHasType(cToken, "REPORT_DISPUTED");
        assertHasType(wToken, "REPORT_DISPUTED");
    }

    private void assertHasType(String token, String type) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = MAPPER.readTree(result.getResponse().getContentAsString()).get("content");
        for (JsonNode item : content) {
            if (type.equals(item.get("type").asText())) {
                return;
            }
        }
        org.junit.jupiter.api.Assertions.fail("Expected a " + type + " notification");
    }

    // --- mark-all-read scoping ---

    @Test
    void markAllReadOnlyAffectsTheCallingUser() throws Exception {
        User alice = seedUser(User.Role.CITIZEN);
        User bob = seedUser(User.Role.CITIZEN);
        String aToken = bearer(alice);
        String bToken = bearer(bob);
        createReport(aToken, seedCategory(), "Alice report");
        createReport(bToken, seedCategory(), "Bob report");

        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header("Authorization", aToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", aToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", bToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }
}
