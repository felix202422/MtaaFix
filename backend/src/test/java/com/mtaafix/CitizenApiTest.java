package com.mtaafix;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtaafix.report.repository.ReportCategoryRepository;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
import java.util.UUID;
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

@SpringBootTest(classes = MtaaFixApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CitizenApiTest {

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

    private User seedUser(User.Role role) {
        return userRepository.save(new User("c7-" + System.nanoTime() + "@mtaafix.test",
                BCrypt.hashpw(PASSWORD, BCrypt.gensalt()), "Citizen Seven", "+255733333333",
                role));
    }

    private String seedCategory() {
        String suffix = System.nanoTime() + "";
        return categoryRepository.save(new com.mtaafix.report.domain.ReportCategory(
                "Citizen category " + suffix, "c7-cat-" + suffix,
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

    // --- PUT /auth/me ---

    @Test
    void profileUpdateChangesNameAndMobileOnly() throws Exception {
        User user = seedUser(User.Role.CITIZEN);
        String token = bearer(user);
        mockMvc.perform(put("/api/v1/auth/me")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated Name\",\"mobile\":\"+255700000001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.mobile").value("+255700000001"))
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    @Test
    void profileUpdateRejectsBlankName() throws Exception {
        User user = seedUser(User.Role.CITIZEN);
        String token = bearer(user);
        mockMvc.perform(put("/api/v1/auth/me")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void profileUpdateRequiresAuth() throws Exception {
        mockMvc.perform(put("/api/v1/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /reports/{id}/cancel (T8) ---

    @Test
    void creatorCancelsSubmittedReport() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        String token = bearer(citizen);
        String id = createReport(token, seedCategory(), "Cancel me");
        mockMvc.perform(post("/api/v1/reports/" + id + "/cancel")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
        MvcResult result = mockMvc.perform(get("/api/v1/reports/" + id)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode history = MAPPER.readTree(result.getResponse().getContentAsString())
                .get("statusHistory");
        JsonNode latest = history.get(history.size() - 1);
        org.junit.jupiter.api.Assertions.assertEquals("CANCELLED_BY_CITIZEN",
                latest.get("reasonCode").asText());
    }

    @Test
    void cancelIsForbiddenForNonCreatorAndRequiresSubmittedState() throws Exception {
        User creator = seedUser(User.Role.CITIZEN);
        User other = seedUser(User.Role.CITIZEN);
        String cToken = bearer(creator);
        String oToken = bearer(other);
        String id = createReport(cToken, seedCategory(), "Guarded cancel");

        mockMvc.perform(post("/api/v1/reports/" + id + "/cancel")
                        .header("Authorization", oToken))
                .andExpect(status().isForbidden());

        String modToken = bearer(seedUser(User.Role.MODERATOR));
        mockMvc.perform(post("/api/v1/reports/" + id + "/cancel")
                        .header("Authorization", modToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/reports/" + id + "/cancel")
                        .header("Authorization", cToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reports/" + id + "/cancel")
                        .header("Authorization", cToken))
                .andExpect(status().isConflict());
    }

    // --- multipart upload + content streaming ---

    private static MockMultipartFile jpegPhoto() {
        byte[] bytes = new byte[64];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        bytes[3] = (byte) 0xE0;
        return new MockMultipartFile("file", "photo.jpg", "image/jpeg", bytes);
    }

    @Test
    void uploadStoresPhotoAndContentStreamsBack() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        String token = bearer(citizen);
        String id = createReport(token, seedCategory(), "With photo");

        MvcResult result = mockMvc.perform(multipart("/api/v1/reports/" + id + "/media/upload")
                        .file(jpegPhoto())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.media").isArray())
                .andExpect(jsonPath("$.media.length()").value(1))
                .andReturn();
        String mediaId = MAPPER.readTree(result.getResponse().getContentAsString())
                .get("media").get(0).get("id").asText();

        mockMvc.perform(get("/api/v1/media/" + mediaId + "/content")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
    }

    @Test
    void uploadRejectsBadMimeAndBadMagicBytesAndOversize() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        String token = bearer(citizen);
        String id = createReport(token, seedCategory(), "Upload validation");

        MockMultipartFile gif = new MockMultipartFile("file", "x.gif", "image/gif",
                new byte[] {0x47, 0x49, 0x46, 0x38});
        mockMvc.perform(multipart("/api/v1/reports/" + id + "/media/upload")
                        .file(gif).header("Authorization", token))
                .andExpect(status().isBadRequest());

        MockMultipartFile fakeJpeg = new MockMultipartFile("file", "fake.jpg", "image/jpeg",
                "not-a-jpeg".getBytes());
        mockMvc.perform(multipart("/api/v1/reports/" + id + "/media/upload")
                        .file(fakeJpeg).header("Authorization", token))
                .andExpect(status().isBadRequest());

        byte[] big = new byte[21 * 1024 * 1024];
        big[0] = (byte) 0xFF;
        big[1] = (byte) 0xD8;
        big[2] = (byte) 0xFF;
        MockMultipartFile oversize = new MockMultipartFile("file", "big.jpg", "image/jpeg", big);
        mockMvc.perform(multipart("/api/v1/reports/" + id + "/media/upload")
                        .file(oversize).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contentStreamingIsForbiddenForUnrelatedCitizen() throws Exception {
        User creator = seedUser(User.Role.CITIZEN);
        User other = seedUser(User.Role.CITIZEN);
        String cToken = bearer(creator);
        String id = createReport(cToken, seedCategory(), "Private evidence");
        MvcResult result = mockMvc.perform(multipart("/api/v1/reports/" + id + "/media/upload")
                        .file(jpegPhoto())
                        .header("Authorization", cToken))
                .andExpect(status().isOk())
                .andReturn();
        String mediaId = MAPPER.readTree(result.getResponse().getContentAsString())
                .get("media").get(0).get("id").asText();

        mockMvc.perform(get("/api/v1/media/" + mediaId + "/content")
                        .header("Authorization", bearer(other)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/media/" + mediaId + "/content"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteRemovesMetadataAndFile() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        String token = bearer(citizen);
        String id = createReport(token, seedCategory(), "Delete photo");
        MvcResult result = mockMvc.perform(multipart("/api/v1/reports/" + id + "/media/upload")
                        .file(jpegPhoto())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode media = MAPPER.readTree(result.getResponse().getContentAsString()).get("media")
                .get(0);
        String mediaId = media.get("id").asText();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/media/" + mediaId)
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/media/" + mediaId + "/content")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    // --- dashboard attention data ---

    @Test
    void dashboardExposesRealAttentionCounts() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        String cToken = bearer(citizen);
        String mToken = bearer(moderator);
        String categoryId = seedCategory();

        String needsInfoId = createReport(cToken, categoryId, "Needs info report");
        String resolvedId = createReport(cToken, categoryId, "Resolved report");

        mockMvc.perform(post("/api/v1/reports/" + needsInfoId + "/submit")
                        .header("Authorization", mToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reports/" + needsInfoId + "/needs-info")
                        .header("Authorization", mToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Please add a photo.\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/reports/" + resolvedId + "/submit")
                        .header("Authorization", mToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reports/" + resolvedId + "/verify")
                        .header("Authorization", mToken))
                .andExpect(status().isOk());
        User worker = seedWorker();
        String adminToken = bearer(seedUser(User.Role.ADMINISTRATOR));
        mockMvc.perform(post("/api/v1/reports/" + resolvedId + "/assign")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + worker.getId() + "\"}"))
                .andExpect(status().isOk());
        String workerToken = bearer(worker);
        mockMvc.perform(post("/api/v1/reports/" + resolvedId + "/start")
                        .header("Authorization", workerToken))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/api/v1/reports/" + resolvedId + "/media/upload")
                        .file(jpegPhoto()).header("Authorization", workerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reports/" + resolvedId + "/complete")
                        .header("Authorization", workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completionNote\":\"Fixed.\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reports/" + resolvedId + "/resolve")
                        .header("Authorization", mToken))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/dashboard/me")
                        .header("Authorization", cToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsInfoCount").value(1))
                .andExpect(jsonPath("$.resolvedAwaitingConfirmCount").value(1))
                .andExpect(jsonPath("$.activeCount").value(2))
                .andExpect(jsonPath("$.attentionReports.length()").value(2))
                .andReturn();
        JsonNode attention = MAPPER.readTree(result.getResponse().getContentAsString())
                .get("attentionReports");
        for (JsonNode node : attention) {
            String statusText = node.get("status").asText();
            org.junit.jupiter.api.Assertions.assertTrue(
                    statusText.equals("NEEDS_INFORMATION") || statusText.equals("RESOLVED"),
                    "attention list must contain only needs-info or resolved reports");
        }
    }

    private User seedWorker() {
        return userRepository.save(new User("fw-" + System.nanoTime() + "@mtaafix.test",
                BCrypt.hashpw(PASSWORD, BCrypt.gensalt()), "Field Worker", "+255744444444",
                User.Role.FIELD_WORKER));
    }

    @Test
    void unknownMediaContentReturns404() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        String token = bearer(citizen);
        mockMvc.perform(get("/api/v1/media/" + UUID.randomUUID() + "/content")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    // --- Phase 8: geographic persistence & FR-11.5 GeoJSON ---

    @Test
    void creatingReportAtNullIslandIsRejected() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        String token = bearer(citizen);
        String categoryId = seedCategory();
        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + categoryId + "\","
                                + "\"title\":\"Null island report\","
                                + "\"description\":\"at 0,0\","
                                + "\"location\":{\"latitude\":0.0,\"longitude\":0.0,\"srid\":4326}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authorizedReportExposesExactLocationAndGeoJson() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        String token = bearer(citizen);
        String categoryId = seedCategory();
        MvcResult result = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + categoryId + "\","
                                + "\"title\":\"GeoJSON test report\","
                                + "\"description\":\"exact location visible to owner\","
                                + "\"location\":{\"latitude\":-6.82,\"longitude\":39.21,\"srid\":4326}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.location.latitude").value(-6.82))
                .andExpect(jsonPath("$.location.longitude").value(39.21))
                .andExpect(jsonPath("$.geoJson.type").value("Point"))
                .andExpect(jsonPath("$.geoJson.coordinates[0]").value(39.21))
                .andExpect(jsonPath("$.geoJson.coordinates[1]").value(-6.82))
                .andReturn();
        JsonNode body = MAPPER.readTree(result.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(-6.82,
                body.get("location").get("latitude").asDouble(), 1e-9);
    }
}
