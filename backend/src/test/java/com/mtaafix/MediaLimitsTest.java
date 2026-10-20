package com.mtaafix;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtaafix.report.repository.ReportCategoryRepository;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
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

/**
 * Phase 13 media hardening: configurable size limit, per-report attachment
 * cap, JSON-attach URL validation, and lifecycle retention (NFR-6: media is
 * retained with the report).
 */
@SpringBootTest(classes = MtaaFixApplication.class, properties = {
        "media.max-per-report=2",
        "media.max-file-size-bytes=1048576"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaLimitsTest {

    private static final String PASSWORD = "password123";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReportCategoryRepository categoryRepository;

    private User seedUser(User.Role role) {
        return userRepository.save(new User("ml-" + System.nanoTime() + "@mtaafix.test",
                BCrypt.hashpw(PASSWORD, BCrypt.gensalt()), "Media Limits", "+255766666666",
                role));
    }

    private String seedCategory() {
        String suffix = System.nanoTime() + "";
        return categoryRepository.save(new com.mtaafix.report.domain.ReportCategory(
                "Media category " + suffix, "ml-cat-" + suffix,
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

    private String createReport(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + seedCategory()
                                + "\",\"title\":\"Media limits\","
                                + "\"description\":\"desc\","
                                + "\"location\":{\"latitude\":-6.82,\"longitude\":39.21,\"srid\":4326}}"))
                .andExpect(status().isCreated())
                .andReturn();
        return MAPPER.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private static MockMultipartFile jpegPhoto() {
        byte[] bytes = new byte[64];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        bytes[3] = (byte) 0xE0;
        return new MockMultipartFile("file", "photo.jpg", "image/jpeg", bytes);
    }

    private void uploadOk(String token, String reportId) throws Exception {
        mockMvc.perform(multipart("/api/v1/reports/" + reportId + "/media/upload")
                        .file(jpegPhoto())
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    void perReportAttachmentCapIsEnforced() throws Exception {
        String token = bearer(seedUser(User.Role.CITIZEN));
        String id = createReport(token);
        uploadOk(token, id);
        uploadOk(token, id);
        mockMvc.perform(multipart("/api/v1/reports/" + id + "/media/upload")
                        .file(jpegPhoto())
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Report already has the maximum of 2 attachments (FR-9.1)."));
    }

    @Test
    void sizeLimitComesFromConfigurationNotAConstant() throws Exception {
        String token = bearer(seedUser(User.Role.CITIZEN));
        String id = createReport(token);
        byte[] big = new byte[1_200_000];
        big[0] = (byte) 0xFF;
        big[1] = (byte) 0xD8;
        big[2] = (byte) 0xFF;
        big[3] = (byte) 0xE0;
        mockMvc.perform(multipart("/api/v1/reports/" + id + "/media/upload")
                        .file(new MockMultipartFile("file", "big.jpg", "image/jpeg", big))
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Media size must not exceed 1048576 bytes (FR-9.2, configurable via MEDIA_MAX_FILE_SIZE_BYTES)."));
    }

    @Test
    void jsonAttachRequiresAContentPathUrl() throws Exception {
        String token = bearer(seedUser(User.Role.CITIZEN));
        String id = createReport(token);

        mockMvc.perform(post("/api/v1/reports/" + id + "/media")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"a.jpg\",\"originalName\":\"a.jpg\","
                                + "\"contentType\":\"image/jpeg\",\"sizeBytes\":100,"
                                + "\"url\":\"https://evil.example/a.jpg\",\"mediaType\":\"PHOTO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "url must reference an uploaded media content path (/api/v1/media/{id}/content)."));

        mockMvc.perform(post("/api/v1/reports/" + id + "/media")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"a.jpg\",\"originalName\":\"a.jpg\","
                                + "\"contentType\":\"image/jpeg\",\"sizeBytes\":100,"
                                + "\"url\":\"/media/a.jpg\",\"mediaType\":\"PHOTO\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/reports/" + id + "/media")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"a.jpg\",\"originalName\":\"a.jpg\","
                                + "\"contentType\":\"image/jpeg\",\"sizeBytes\":100,"
                                + "\"url\":\"/api/v1/media/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/content\","
                                + "\"mediaType\":\"PHOTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.media", hasSize(1)));
    }

    @Test
    void mediaSurvivesLifecycleTransitionsAndStaysAuthorized() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        User stranger = seedUser(User.Role.CITIZEN);
        String cToken = bearer(citizen);
        String mToken = bearer(moderator);
        String sToken = bearer(stranger);
        String id = createReport(cToken);
        uploadOk(cToken, id);

        MvcResult before = mockMvc.perform(get("/api/v1/reports/" + id)
                        .header("Authorization", cToken))
                .andExpect(status().isOk())
                .andReturn();
        String mediaId = MAPPER.readTree(before.getResponse().getContentAsString())
                .get("media").get(0).get("id").asText();

        mockMvc.perform(post("/api/v1/reports/" + id + "/cancel")
                        .header("Authorization", cToken))
                .andExpect(status().isOk());

        // Retention policy NFR-6: media is retained with the report.
        mockMvc.perform(get("/api/v1/reports/" + id)
                        .header("Authorization", cToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.media", hasSize(1)));
        mockMvc.perform(get("/api/v1/media/" + mediaId + "/content")
                        .header("Authorization", cToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/media/" + mediaId + "/content")
                        .header("Authorization", mToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/media/" + mediaId + "/content")
                        .header("Authorization", sToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/media/" + mediaId + "/content"))
                .andExpect(status().isUnauthorized());
    }
}
