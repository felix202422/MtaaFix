package com.mtaafix;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtaafix.audit.repository.AuditRepository;
import com.mtaafix.report.domain.Assignment;
import com.mtaafix.report.domain.Organisation;
import com.mtaafix.report.domain.Report;
import com.mtaafix.report.repository.AssignmentRepository;
import com.mtaafix.report.repository.OrganisationRepository;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Phase 9 (P14-P24): administration and moderation endpoints. Covers FR-5.1,
 * FR-5.3, FR-5.4, FR-7.3, FR-10.x, FR-12.1-12.4 and matrix areas 8/9/12.
 */
@SpringBootTest(classes = MtaaFixApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdministrationApiTest {

    private static final String PASSWORD = "password123";
    private static final org.locationtech.jts.geom.GeometryFactory GF =
            new org.locationtech.jts.geom.GeometryFactory();

    @Autowired
    private MockMvc mockMvc;

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
    private com.mtaafix.user.repository.OrganisationMembershipRepository membershipRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User seedUser(User.Role role) {
        return seedUser(role, null);
    }

    private User seedUser(User.Role role, String organisationId) {
        User user = new User("admin-api-" + System.nanoTime() + "@mtaafix.test",
                BCrypt.hashpw(PASSWORD, BCrypt.gensalt()), "Admin API User",
                "+255722222222", role);
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
        return "Bearer " + node.get("accessToken").asText();
    }

    private Organisation seedOrganisation() {
        Organisation org = new Organisation("Org " + System.nanoTime(),
                "org-" + System.nanoTime());
        return organisationRepository.save(org);
    }

    private com.mtaafix.report.domain.ReportCategory seedCategory() {
        com.mtaafix.report.domain.ReportCategory category =
                new com.mtaafix.report.domain.ReportCategory(
                        "Admin category " + System.nanoTime(),
                        "admin-cat-" + System.nanoTime(),
                        com.mtaafix.report.domain.ReportCategory.Type.PRIMARY);
        return categoryRepository.save(category);
    }

    private Report seedReport(User owner, Report.Status status, Organisation organisation) {
        org.locationtech.jts.geom.Point point =
                GF.createPoint(new org.locationtech.jts.geom.Coordinate(39.2084, -6.8229));
        point.setSRID(4326);
        Report report = new Report(seedCategory(), "Admin report " + System.nanoTime(),
                "Seeded for administration tests", point, null, owner);
        report.setCode("MTF-" + System.nanoTime());
        report.setStatus(status);
        report.setOrganisation(organisation);
        return reportRepository.save(report);
    }

    @Test
    void administrationEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/organizations")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/admin")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/analytics/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/settings")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/audit")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/categories?includeInactive=true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userSearchRequiresAdministratorAndSupportsFilters() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User worker = seedUser(User.Role.FIELD_WORKER);
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);

        mockMvc.perform(get("/api/v1/users").header("Authorization", login(citizen)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users").header("Authorization", login(moderator)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users").header("Authorization", login(worker)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users")
                        .param("q", citizen.getEmail())
                        .header("Authorization", login(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(citizen.getId()))
                .andExpect(jsonPath("$.content[0].active").value(true));

        mockMvc.perform(get("/api/v1/users")
                        .param("role", "FIELD_WORKER")
                        .header("Authorization", login(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/v1/users")
                        .param("role", "NOT_A_ROLE")
                        .header("Authorization", login(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void roleChangesAreAuditedAndEnforceMembershipInvariants() throws Exception {
        User admin = seedUser(User.Role.ADMINISTRATOR);
        User citizen = seedUser(User.Role.CITIZEN);
        String token = login(admin);

        mockMvc.perform(put("/api/v1/users/" + citizen.getId() + "/role")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MODERATOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MODERATOR"));
        org.junit.jupiter.api.Assertions.assertTrue(
                auditRepository.findByEntityId(citizen.getId()).stream()
                        .anyMatch(e -> "USER_ROLE_CHANGED".equals(e.getAction())
                                && "SUCCESS".equals(e.getOutcome())),
                "Role change must be audited with outcome SUCCESS (FR-12.4)");

        User plainWorker = seedUser(User.Role.CITIZEN);
        mockMvc.perform(put("/api/v1/users/" + plainWorker.getId() + "/role")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"FIELD_WORKER\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/users/" + admin.getId() + "/role")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CITIZEN\"}"))
                .andExpect(status().isBadRequest());

        Organisation team = seedOrganisation();
        User member = seedUser(User.Role.CITIZEN);
        mockMvc.perform(post("/api/v1/organizations/" + team.getId() + "/members")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + member.getId()
                                + "\",\"role\":\"FIELD_WORKER\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/v1/users/" + member.getId() + "/role")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CITIZEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CITIZEN"));
        org.junit.jupiter.api.Assertions.assertFalse(
                membershipRepository.findById(member.getId()).isPresent(),
                "Changing away from an organisation role must remove the membership");
    }

    @Test
    void accountStatusChangesEnforceSelfAndLastAdministratorGuards() throws Exception {
        User adminA = seedUser(User.Role.ADMINISTRATOR);
        User adminB = seedUser(User.Role.ADMINISTRATOR);
        User victim = seedUser(User.Role.CITIZEN);
        String token = login(adminA);

        userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.ADMINISTRATOR && u.isActive()
                        && !u.getId().equals(adminA.getId()) && !u.getId().equals(adminB.getId()))
                .forEach(u -> {
                    u.setRole(User.Role.CITIZEN);
                    userRepository.save(u);
                });

        mockMvc.perform(put("/api/v1/users/" + adminA.getId() + "/active")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/users/" + victim.getId() + "/active")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        org.junit.jupiter.api.Assertions.assertTrue(
                auditRepository.findByEntityId(victim.getId()).stream()
                        .anyMatch(e -> "USER_DEACTIVATED".equals(e.getAction())),
                "Deactivation must be audited (FR-12.1)");

        mockMvc.perform(get("/api/v1/users")
                        .param("q", victim.getEmail())
                        .param("active", "false")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].active").value(false));

        mockMvc.perform(put("/api/v1/users/" + adminB.getId() + "/active")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/users/" + adminB.getId() + "/role")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MODERATOR\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void organisationLifecycleRequiresAdministratorAndAudits() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User orgAdmin = seedUser(User.Role.ORGANIZATION_ADMIN);
        User admin = seedUser(User.Role.ADMINISTRATOR);
        String token = login(admin);
        String slug = "org-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", login(citizen))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Citizen Org\",\"slug\":\"citizen-org\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", login(orgAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Staff Org\",\"slug\":\"staff-org\"}"))
                .andExpect(status().isForbidden());

        MvcResult created = mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Phase 9 Org\",\"slug\":\"" + slug + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value(slug))
                .andReturn();
        String orgId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();
        org.junit.jupiter.api.Assertions.assertTrue(
                auditRepository.findByEntityId(orgId).stream()
                        .anyMatch(e -> "ORGANIZATION_CREATED".equals(e.getAction())),
                "Organisation creation must be audited (FR-10.1)");

        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad Slug\",\"slug\":\"Not A Slug\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dup\",\"slug\":\"" + slug + "\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/v1/organizations/" + orgId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed Org\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Org"));

        mockMvc.perform(put("/api/v1/organizations/" + orgId + "/active")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        org.junit.jupiter.api.Assertions.assertTrue(
                auditRepository.findByEntityId(orgId).stream()
                        .anyMatch(e -> "ORGANIZATION_DEACTIVATED".equals(e.getAction())),
                "Organisation deactivation must be audited (FR-10.1)");

        User owner = seedUser(User.Role.CITIZEN);
        Report verified = seedReport(owner, Report.Status.VERIFIED,
                organisationRepository.findById(orgId).orElseThrow());
        User worker = seedUser(User.Role.FIELD_WORKER);
        mockMvc.perform(post("/api/v1/reports/" + verified.getId() + "/assign")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + worker.getId()
                                + "\",\"organisationId\":\"" + orgId + "\"}"))
                .andExpect(status().isConflict());

        User orgAdminA = seedUser(User.Role.ORGANIZATION_ADMIN, orgId);
        mockMvc.perform(get("/api/v1/organizations")
                        .header("Authorization", login(orgAdminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers
                        .hasItem(orgId)));
        mockMvc.perform(get("/api/v1/organizations")
                        .header("Authorization", login(citizen)))
                .andExpect(status().isForbidden());
    }

    @Test
    void membershipAddGrantsRoleAndRemoveKeepsIt() throws Exception {
        User admin = seedUser(User.Role.ADMINISTRATOR);
        Organisation orgA = seedOrganisation();
        Organisation orgB = seedOrganisation();
        User orgAdminA = seedUser(User.Role.ORGANIZATION_ADMIN, orgA.getId());
        User orgAdminB = seedUser(User.Role.ORGANIZATION_ADMIN, orgB.getId());
        User recruit = seedUser(User.Role.CITIZEN);
        String token = login(admin);

        mockMvc.perform(post("/api/v1/organizations/" + orgA.getId() + "/members")
                        .header("Authorization", login(orgAdminB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + recruit.getId()
                                + "\",\"role\":\"FIELD_WORKER\"}"))
                .andExpect(status().isForbidden());

        MvcResult added = mockMvc.perform(post("/api/v1/organizations/" + orgA.getId()
                        + "/members")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + recruit.getId()
                                + "\",\"role\":\"FIELD_WORKER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("FIELD_WORKER"))
                .andReturn();
        org.junit.jupiter.api.Assertions.assertNotNull(added);
        org.junit.jupiter.api.Assertions.assertTrue(
                auditRepository.findByEntityId(recruit.getId()).stream()
                        .anyMatch(e -> "USER_ROLE_CHANGED".equals(e.getAction())),
                "Member add must audit the role grant (FR-5.4)");
        org.junit.jupiter.api.Assertions.assertTrue(
                auditRepository.findByEntityId(orgA.getId()).stream()
                        .anyMatch(e -> "MEMBERSHIP_ADDED".equals(e.getAction())),
                "Member add must audit the membership (B-X.3)");
        org.junit.jupiter.api.Assertions.assertEquals(User.Role.FIELD_WORKER,
                userRepository.findById(recruit.getId()).orElseThrow().getRole());

        mockMvc.perform(post("/api/v1/organizations/" + orgA.getId() + "/members")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + recruit.getId() + "\",\"role\":\"CITIZEN\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/organizations/" + orgA.getId() + "/members")
                        .header("Authorization", login(orgAdminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/v1/assignments/me")
                        .header("Authorization", login(recruit)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/organizations/" + orgA.getId() + "/members/"
                        + recruit.getId())
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertFalse(
                membershipRepository.findById(recruit.getId()).isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(User.Role.FIELD_WORKER,
                userRepository.findById(recruit.getId()).orElseThrow().getRole(),
                "Member removal keeps the granted role (documented behaviour)");
        mockMvc.perform(get("/api/v1/organizations/" + orgA.getId() + "/members")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].userId").value(org.hamcrest.Matchers
                        .not(org.hamcrest.Matchers.hasItem(recruit.getId()))));
    }

    @Test
    void categoryAdministrationHidesInactiveFromPickers() throws Exception {
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);
        User citizen = seedUser(User.Role.CITIZEN);
        String token = login(admin);
        String slug = "admin-cat-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", login(moderator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nope\",\"slug\":\"nope-" + System.nanoTime()
                                + "\"}"))
                .andExpect(status().isForbidden());

        MvcResult created = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Admin category\",\"slug\":\"" + slug + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        String categoryId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dup\",\"slug\":\"" + slug + "\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/v1/categories/" + categoryId + "/order")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayOrder\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayOrder").value(7));
        org.junit.jupiter.api.Assertions.assertTrue(
                auditRepository.findByEntityId(categoryId).stream()
                        .anyMatch(e -> "CATEGORY_REORDERED".equals(e.getAction())),
                "Reordering must be audited (FR-5.1)");

        mockMvc.perform(put("/api/v1/categories/" + categoryId + "/active")
                        .param("value", "false")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        org.junit.jupiter.api.Assertions.assertTrue(
                auditRepository.findByEntityId(categoryId).stream()
                        .anyMatch(e -> "CATEGORY_DEACTIVATED".equals(e.getAction())
                                && "SUCCESS".equals(e.getOutcome())),
                "Deactivation must be audited (FR-5.1)");

        mockMvc.perform(get("/api/v1/categories/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers
                        .not(org.hamcrest.Matchers.hasItem(categoryId))));
        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", login(citizen)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers
                        .not(org.hamcrest.Matchers.hasItem(categoryId))));
        mockMvc.perform(get("/api/v1/categories?includeInactive=true")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers
                        .hasItem(categoryId)));
        mockMvc.perform(get("/api/v1/categories?includeInactive=true")
                        .header("Authorization", login(citizen)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", login(citizen))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + categoryId
                                + "\",\"title\":\"Should fail\","
                                + "\"description\":\"deactivated category\","
                                + "\"location\":{\"latitude\":-6.82,\"longitude\":39.21,"
                                + "\"srid\":4326}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportListSupportsTextAndAssignmentFilters() throws Exception {
        User admin = seedUser(User.Role.ADMINISTRATOR);
        User owner = seedUser(User.Role.CITIZEN);
        User stranger = seedUser(User.Role.CITIZEN);
        User worker = seedUser(User.Role.FIELD_WORKER);
        String token = login(admin);
        String tokenWord = "P9TOKEN" + System.nanoTime();

        Report unassigned = seedReport(owner, Report.Status.SUBMITTED, null);
        unassigned.setTitle(tokenWord + " unassigned");
        unassigned = reportRepository.save(unassigned);

        Report assigned = seedReport(stranger, Report.Status.ASSIGNED, null);
        assigned.setTitle(tokenWord + " assigned");
        assigned.setAssignment(
                assignmentRepository.save(new Assignment(assigned, worker, null)));
        assigned = reportRepository.save(assigned);

        mockMvc.perform(get("/api/v1/reports")
                        .param("q", tokenWord)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/v1/reports")
                        .param("assignment", "assigned")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers
                        .hasItem(assigned.getId())))
                .andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers
                        .not(org.hamcrest.Matchers.hasItem(unassigned.getId()))));

        mockMvc.perform(get("/api/v1/reports")
                        .param("assignment", "unassigned")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers
                        .hasItem(unassigned.getId())));

        mockMvc.perform(get("/api/v1/reports")
                        .param("assignment", "both")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/reports")
                        .param("status", "NOT_A_STATUS")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/reports")
                        .header("Authorization", login(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers
                        .not(org.hamcrest.Matchers.hasItem(assigned.getId()))))
                .andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers
                        .hasItem(unassigned.getId())));

        mockMvc.perform(get("/api/v1/reports")
                        .header("Authorization", login(worker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers
                        .hasItem(assigned.getId())));
    }

    @Test
    void adminDashboardServesStaffOnlyAndReturnsRealPanels() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User worker = seedUser(User.Role.FIELD_WORKER);
        User orgAdmin = seedUser(User.Role.ORGANIZATION_ADMIN);
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);

        mockMvc.perform(get("/api/v1/dashboard/admin")
                        .header("Authorization", login(citizen)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/dashboard/admin")
                        .header("Authorization", login(worker)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/dashboard/admin")
                        .header("Authorization", login(orgAdmin)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/dashboard/admin")
                        .header("Authorization", login(moderator)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dashboard/admin")
                        .header("Authorization", login(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").isNumber())
                .andExpect(jsonPath("$.byStatus").isMap())
                .andExpect(jsonPath("$.activity14d").isArray())
                .andExpect(jsonPath("$.activity14d", hasSize(14)))
                .andExpect(jsonPath("$.moderationQueue").isMap())
                .andExpect(jsonPath("$.workload").isMap())
                .andExpect(jsonPath("$.attention").isMap())
                .andExpect(jsonPath("$.recent").isArray())
                .andExpect(jsonPath("$.byCategory").isArray());
    }

    @Test
    void auditLogFiltersByActionActorEntityAndTime() throws Exception {
        User admin = seedUser(User.Role.ADMINISTRATOR);
        User moderator = seedUser(User.Role.MODERATOR);
        String token = login(admin);
        String slug = "audit-cat-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Audit category\",\"slug\":\"" + slug + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/audit")
                        .param("action", "CATEGORY_CREATED")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].action")
                        .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers
                                .is("CATEGORY_CREATED"))));

        mockMvc.perform(get("/api/v1/audit")
                        .param("actorId", admin.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].actorId")
                        .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers
                                .is(admin.getId()))))
                .andExpect(jsonPath("$.content[*].actorEmail")
                        .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers
                                .is(admin.getEmail()))));

        mockMvc.perform(get("/api/v1/audit")
                        .param("entityType", "ReportCategory")
                        .param("action", "CATEGORY_CREATED")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].entityType")
                        .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers
                                .is("ReportCategory"))));

        mockMvc.perform(get("/api/v1/audit")
                        .param("action", "NO_SUCH_ACTION_123")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/audit")
                        .param("from", "2099-01-01T00:00:00Z")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/audit")
                        .param("from", "not-a-timestamp")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/audit")
                        .param("outcome", "SUCCESS")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/v1/audit")
                        .header("Authorization", login(moderator)))
                .andExpect(status().isForbidden());
    }

    @Test
    void analyticsEnforcesPermissionsAndValidatesParameters() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User worker = seedUser(User.Role.FIELD_WORKER);
        User orgAdmin = seedUser(User.Role.ORGANIZATION_ADMIN);
        User admin = seedUser(User.Role.ADMINISTRATOR);
        Organisation otherOrg = seedOrganisation();

        mockMvc.perform(get("/api/v1/analytics/summary")
                        .header("Authorization", login(citizen)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/analytics/summary")
                        .header("Authorization", login(worker)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/analytics/summary")
                        .param("orgId", otherOrg.getId())
                        .header("Authorization", login(orgAdmin)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/analytics/summary")
                        .header("Authorization", login(orgAdmin)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/analytics/summary")
                        .param("status", "NOT_A_STATUS")
                        .header("Authorization", login(admin)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/analytics/summary")
                        .param("bbox", "not-a-bbox")
                        .header("Authorization", login(admin)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/analytics/summary")
                        .param("from", "2026-02-01T00:00:00Z")
                        .param("to", "2026-01-01T00:00:00Z")
                        .header("Authorization", login(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void settingsAreAdminOnlyAndNeverExposeSecrets() throws Exception {
        User citizen = seedUser(User.Role.CITIZEN);
        User moderator = seedUser(User.Role.MODERATOR);
        User admin = seedUser(User.Role.ADMINISTRATOR);

        mockMvc.perform(get("/api/v1/settings")
                        .header("Authorization", login(citizen)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/settings")
                        .header("Authorization", login(moderator)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/settings")
                        .header("Authorization", login(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.general.applicationName").isNotEmpty())
                .andExpect(jsonPath("$.reporting.maxUploadBytes").value(20971520L))
                .andExpect(jsonPath("$.security.accessTokenTtlSeconds").value(900L))
                .andExpect(jsonPath("$.geographic.fuzzMinMeters").value(150))
                .andExpect(jsonPath("$.geographic.fuzzMaxMeters").value(600))
                .andExpect(jsonPath("$.geographic.fuzzSecretIsDefault").value(true))
                .andExpect(jsonPath("$.geographic.fuzzSecretConfigured").value(false))
                .andExpect(jsonPath("$.privacy.locationFuzzing").isNotEmpty())
                .andExpect(jsonPath("$.integrations.mapTiles").isNotEmpty());
    }

    @Test
    void settingsNeverLeakRawSecretMaterial() throws Exception {
        User admin = seedUser(User.Role.ADMINISTRATOR);
        MvcResult result = mockMvc.perform(get("/api/v1/settings")
                        .header("Authorization", login(admin)))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("min-32-char-secret"),
                "JWT secret material must never appear in the settings payload");
        org.junit.jupiter.api.Assertions.assertFalse(
                body.contains("mtaafix-local-dev-geo-fuzz-secret"),
                "GEO_FUZZ_SECRET must never appear in the settings payload (FR-11.4)");
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("password"),
                "Datasource password material must never appear in settings");
    }
}
