package com.mtaafix;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtaafix.audit.repository.AuditRepository;
import com.mtaafix.notification.domain.Notification;
import com.mtaafix.notification.repository.NotificationRepository;
import com.mtaafix.report.domain.Assignment;
import com.mtaafix.report.domain.Organisation;
import com.mtaafix.report.domain.Report;
import com.mtaafix.report.repository.AssignmentRepository;
import com.mtaafix.report.repository.OrganisationRepository;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
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
class AuthorizationMatrixTest {

    private static final String PASSWORD = "password123";
    private static final org.locationtech.jts.geom.GeometryFactory GF =
            new org.locationtech.jts.geom.GeometryFactory();

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private com.mtaafix.report.repository.ReportCategoryRepository categoryRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private com.mtaafix.user.repository.OrganisationMembershipRepository membershipRepository;

    private User seedUser(User.Role role) {
        return seedUser(role, null);
    }

    private User seedUser(User.Role role, String organisationId) {
        User user = new User("matrix-" + System.nanoTime() + "@mtaafix.test",
                BCrypt.hashpw(PASSWORD, BCrypt.gensalt()), "Matrix User", "+255711111111", role);
        user = userRepository.save(user);
        if (organisationId != null) {
            membershipRepository.save(new com.mtaafix.user.domain.OrganisationMembership(
                    user.getId(), organisationId, role));
        }
        return user;
    }

    private String login(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail()
                                + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("accessToken").asText();
    }

    private Report seedReport(User owner, Report.Status status) {
        return seedReport(owner, status, null);
    }

    private Report seedReport(User owner, Report.Status status, Organisation organisation) {
        org.locationtech.jts.geom.Point point =
                GF.createPoint(new org.locationtech.jts.geom.Coordinate(39.2084, -6.8229));
        point.setSRID(4326);
        com.mtaafix.report.domain.ReportCategory category =
                categoryRepository.save(new com.mtaafix.report.domain.ReportCategory(
                        "Matrix category " + System.nanoTime(),
                        "matrix-cat-" + System.nanoTime(),
                        com.mtaafix.report.domain.ReportCategory.Type.PRIMARY));
        Report report = new Report(category, "Matrix report " + System.nanoTime(),
                "Seeded for authorization matrix", point, null, owner);
        report.setCode("MTF-" + System.nanoTime());
        report.setStatus(status);
        report.setOrganisation(organisation);
        return reportRepository.save(report);
    }

    private Organisation seedOrganisation() {
        Organisation org = new Organisation("Org " + System.nanoTime(),
                "org-" + System.nanoTime());
        return organisationRepository.save(org);
    }

    private String bearer(User user) throws Exception {
        return "Bearer " + login(user);
    }

    @Test
    void unauthenticatedRequestsAreRejectedWith401() throws Exception {
        User owner = seedUser(User.Role.CITIZEN);
        Report report = seedReport(owner, Report.Status.SUBMITTED);

        mockMvc.perform(get("/api/v1/reports/" + report.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/stats"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"slug\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void categoryCreationRequiresAdministrator() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);
        String slug = "cat-" + System.nanoTime();
        String body = "{\"name\":\"Potholes\",\"slug\":\"" + slug + "\",\"type\":\"PRIMARY\"}";

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", bearer(citizen))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", bearer(moderator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        MvcResult created = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        JsonNode node = objectMapper.readTree(created.getResponse().getContentAsString());
        String categoryId = node.get("id").asText();
        auditRepository.findByEntityId(categoryId).forEach(entry -> {
            org.junit.jupiter.api.Assertions.assertEquals("CATEGORY_CREATED", entry.getAction());
            org.junit.jupiter.api.Assertions.assertEquals(admin.getId(), entry.getActorId());
        });
        org.junit.jupiter.api.Assertions.assertFalse(
                auditRepository.findByEntityId(categoryId).isEmpty(),
                "Category creation must write an audit entry (FR-10.1)");
    }

    @Test
    void globalAuditRequiresAdministrator() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);

        mockMvc.perform(get("/api/v1/audit")
                        .header("Authorization", bearer(citizen)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/audit")
                        .header("Authorization", bearer(moderator)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/audit")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void orgAuditIsScopedToOwnOrganisation() throws Exception {
        Organisation orgA = seedOrganisation();
        Organisation orgB = seedOrganisation();
        User orgAdminA = seedUser(User.Role.ORGANIZATION_ADMIN, orgA.getId());
        User orgAdminB = seedUser(User.Role.ORGANIZATION_ADMIN, orgB.getId());
        User admin = seedUser(User.Role.ADMINISTRATOR);
        User citizen = seedUser(User.Role.CITIZEN);

        mockMvc.perform(get("/api/v1/audit/organizations/" + orgA.getId())
                        .header("Authorization", bearer(orgAdminA)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/audit/organizations/" + orgA.getId())
                        .header("Authorization", bearer(orgAdminB)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/audit/organizations/" + orgA.getId())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/audit/organizations/" + orgA.getId())
                        .header("Authorization", bearer(citizen)))
                .andExpect(status().isForbidden());
    }

    @Test
    void globalStatsRequireModeratorOrAdministrator() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User fieldWorker = seedUser(User.Role.FIELD_WORKER);
        User orgAdmin = seedUser(User.Role.ORGANIZATION_ADMIN);
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);

        mockMvc.perform(get("/api/v1/dashboard/stats")
                        .header("Authorization", bearer(citizen)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/dashboard/stats")
                        .header("Authorization", bearer(fieldWorker)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/dashboard/stats")
                        .header("Authorization", bearer(orgAdmin)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/dashboard/stats")
                        .header("Authorization", bearer(moderator)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/dashboard/stats")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void orgStatsAreScopedToOwnOrganisation() throws Exception {
        Organisation orgA = seedOrganisation();
        Organisation orgB = seedOrganisation();
        User orgAdminA = seedUser(User.Role.ORGANIZATION_ADMIN, orgA.getId());
        User orgAdminB = seedUser(User.Role.ORGANIZATION_ADMIN, orgB.getId());
        User admin = seedUser(User.Role.ADMINISTRATOR);
        User citizen = seedUser(User.Role.CITIZEN);

        mockMvc.perform(get("/api/v1/dashboard/organizations/" + orgA.getId() + "/stats")
                        .header("Authorization", bearer(orgAdminA)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/dashboard/organizations/" + orgA.getId() + "/stats")
                        .header("Authorization", bearer(orgAdminB)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/dashboard/organizations/" + orgA.getId() + "/stats")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/dashboard/organizations/" + orgA.getId() + "/stats")
                        .header("Authorization", bearer(citizen)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportReadIsScopedByOwnershipRoleAndOrganisation() throws Exception {
        User owner = seedUser(User.Role.CITIZEN);
        User stranger = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        User assignedWorker = seedUser(User.Role.FIELD_WORKER);
        User otherWorker = seedUser(User.Role.FIELD_WORKER);
        Organisation orgA = seedOrganisation();
        Organisation orgB = seedOrganisation();
        User orgAdminA = seedUser(User.Role.ORGANIZATION_ADMIN, orgA.getId());
        User orgAdminB = seedUser(User.Role.ORGANIZATION_ADMIN, orgB.getId());

        Report ownReport = seedReport(owner, Report.Status.SUBMITTED);
        Report assignedReport = seedReport(stranger, Report.Status.ASSIGNED);
        assignedReport.setAssignment(
                assignmentRepository.save(new Assignment(assignedReport, assignedWorker, null)));
        reportRepository.save(assignedReport);
        Report orgReport = seedReport(stranger, Report.Status.VERIFIED, orgA);

        mockMvc.perform(get("/api/v1/reports/" + ownReport.getId())
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/" + ownReport.getId())
                        .header("Authorization", bearer(stranger)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reports/" + ownReport.getId())
                        .header("Authorization", bearer(moderator)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/" + assignedReport.getId())
                        .header("Authorization", bearer(assignedWorker)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/" + assignedReport.getId())
                        .header("Authorization", bearer(otherWorker)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reports/" + orgReport.getId())
                        .header("Authorization", bearer(orgAdminA)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/" + orgReport.getId())
                        .header("Authorization", bearer(orgAdminB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportEditIsRestrictedToCreator() throws Exception {
        User owner = seedUser(User.Role.CITIZEN);
        User stranger = seedUser(User.Role.CITIZEN);
        Report report = seedReport(owner, Report.Status.SUBMITTED);
        String body = "{\"title\":\"Edited title\"}";

        mockMvc.perform(put("/api/v1/reports/" + report.getId())
                        .header("Authorization", bearer(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/reports/" + report.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Edited title"));
    }

    @Test
    void reviewActionsRequireModeratorOrAdministrator() throws Exception {
        User owner = seedUser(User.Role.CITIZEN);
        User fieldWorker = seedUser(User.Role.FIELD_WORKER);
        User moderator = seedUser(User.Role.MODERATOR);

        Report forSubmit = seedReport(owner, Report.Status.SUBMITTED);
        Report forVerify = seedReport(owner, Report.Status.UNDER_REVIEW);
        Report forReject = seedReport(owner, Report.Status.UNDER_REVIEW);

        mockMvc.perform(post("/api/v1/reports/" + forSubmit.getId() + "/submit")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + forVerify.getId() + "/verify")
                        .header("Authorization", bearer(fieldWorker)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + forReject.getId() + "/reject")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reasonCode\":\"NOT_RELEVANT\",\"comment\":\"not relevant\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/reports/" + forSubmit.getId() + "/submit")
                        .header("Authorization", bearer(moderator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
        mockMvc.perform(post("/api/v1/reports/" + forVerify.getId() + "/verify")
                        .header("Authorization", bearer(moderator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    @Test
    void assignmentRequiresAdministratorOrOrgAdminWithOrgScope() throws Exception {
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);
        User assignee = seedUser(User.Role.FIELD_WORKER);
        Organisation orgA = seedOrganisation();
        Organisation orgB = seedOrganisation();
        User orgAdminA = seedUser(User.Role.ORGANIZATION_ADMIN, orgA.getId());
        User orgAdminB = seedUser(User.Role.ORGANIZATION_ADMIN, orgB.getId());
        User owner = seedUser(User.Role.CITIZEN);

        Report verifiedInOrgA = seedReport(owner, Report.Status.VERIFIED, orgA);
        Report verifiedInOrgB = seedReport(owner, Report.Status.VERIFIED, orgB);
        Report verifiedUnscoped = seedReport(owner, Report.Status.VERIFIED);

        mockMvc.perform(post("/api/v1/reports/" + verifiedUnscoped.getId() + "/assign")
                        .header("Authorization", bearer(moderator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + assignee.getId() + "\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/reports/" + verifiedInOrgA.getId() + "/assign")
                        .header("Authorization", bearer(orgAdminB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + assignee.getId()
                                + "\",\"organisationId\":\"" + orgA.getId() + "\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/reports/" + verifiedInOrgA.getId() + "/assign")
                        .header("Authorization", bearer(orgAdminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + assignee.getId()
                                + "\",\"organisationId\":\"" + orgA.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        mockMvc.perform(post("/api/v1/reports/" + verifiedInOrgB.getId() + "/assign")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + assignee.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    void resolveAndCloseFollowLifecycleActorRoles() throws Exception {
        User owner = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);

        Report toResolveAsCitizen = seedReport(owner, Report.Status.RESOLUTION_REVIEW);
        Report toResolveAsModerator = seedReport(owner, Report.Status.RESOLUTION_REVIEW);
        Report toCloseAsModerator = seedReport(owner, Report.Status.RESOLVED);
        Report toCloseAsAdmin = seedReport(owner, Report.Status.RESOLVED);

        mockMvc.perform(post("/api/v1/reports/" + toResolveAsCitizen.getId() + "/resolve")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/" + toCloseAsModerator.getId() + "/close")
                        .header("Authorization", bearer(moderator)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/reports/" + toResolveAsModerator.getId() + "/resolve")
                        .header("Authorization", bearer(moderator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
        mockMvc.perform(post("/api/v1/reports/" + toCloseAsAdmin.getId() + "/close")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void invalidStateTransitionReturns409Conflict() throws Exception {
        User moderator = seedUser(User.Role.MODERATOR);
        Report submitted = seedReport(seedUser(User.Role.CITIZEN), Report.Status.SUBMITTED);

        mockMvc.perform(post("/api/v1/reports/" + submitted.getId() + "/verify")
                        .header("Authorization", bearer(moderator)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void assignmentEndpointsEnforceRoleAndOrgScope() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User fieldWorker = seedUser(User.Role.FIELD_WORKER);
        Organisation orgA = seedOrganisation();
        Organisation orgB = seedOrganisation();
        User orgAdminA = seedUser(User.Role.ORGANIZATION_ADMIN, orgA.getId());
        User orgAdminB = seedUser(User.Role.ORGANIZATION_ADMIN, orgB.getId());
        User admin = seedUser(User.Role.ADMINISTRATOR);

        mockMvc.perform(get("/api/v1/assignments/me")
                        .header("Authorization", bearer(citizen)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/assignments/me")
                        .header("Authorization", bearer(fieldWorker)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/assignments/organizations/" + orgA.getId() + "/reports")
                        .header("Authorization", bearer(orgAdminA)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/assignments/organizations/" + orgA.getId() + "/reports")
                        .header("Authorization", bearer(orgAdminB)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/assignments/organizations/" + orgA.getId() + "/reports")
                        .header("Authorization", bearer(citizen)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/assignments/organizations/" + orgA.getId() + "/reports")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void notificationsAreScopedToTheirOwner() throws Exception {
        User userA = seedUser(User.Role.CITIZEN);
        User userB = seedUser(User.Role.CITIZEN);
        notificationRepository.save(new Notification("A title", "A body",
                Notification.Type.REPORT_SUBMITTED, userA));
        notificationRepository.save(new Notification("B title", "B body",
                Notification.Type.REPORT_SUBMITTED, userB));

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bearer(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("A title"));

        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header("Authorization", bearer(userA)))
                .andExpect(status().isOk());

        List<Notification> unreadOfB = notificationRepository
                .findByUserOrderByCreatedAtDesc(userB).stream()
                .filter(n -> !Boolean.TRUE.equals(n.getRead()))
                .toList();
        org.junit.jupiter.api.Assertions.assertFalse(unreadOfB.isEmpty(),
                "markAllAsRead must not touch other users' notifications");
    }

    @Test
    void swaggerRequiresAdministrator() throws Exception {
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);

        mockMvc.perform(get("/v3/api-docs")
                        .header("Authorization", bearer(moderator)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v3/api-docs")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }
}
