package org.nagrivic.modules.civicgeography;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.civicgeography.dto.admin.CreateCivicBodyRequest;
import org.nagrivic.modules.civicgeography.model.CivicBodyType;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AdminCivicGeographySecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UserEntity citizenUser;
    private String citizenToken;

    private UserEntity moderatorUser;
    private String moderatorToken;

    private UserEntity adminUser;
    private String adminToken;

    private UserEntity createUniqueUser(String name, String role) {
        long uniqueNum = Math.abs(ThreadLocalRandom.current().nextLong(1000000000L, 9999999999L));
        UserEntity u = new UserEntity("+91" + uniqueNum, name);
        u.setRole(role);
        return userRepository.save(u);
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        citizenUser = createUniqueUser("Citizen Arjun", "CITIZEN");
        citizenToken = jwtService.generateAccessToken(citizenUser.getId(), "CITIZEN");

        moderatorUser = createUniqueUser("Moderator Sneha", "MODERATOR");
        moderatorToken = jwtService.generateAccessToken(moderatorUser.getId(), "MODERATOR");

        adminUser = createUniqueUser("Admin Patel", "ADMIN");
        adminToken = jwtService.generateAccessToken(adminUser.getId(), "ADMIN");
    }

    @Test
    @DisplayName("Unauthenticated request to geography admin endpoints is rejected with 401 Unauthorized")
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/admin/geography/overview"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/geography/civic-bodies"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/geography/wards"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/geography/departments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CITIZEN cannot access geography overview, list, or mutations (403 Forbidden)")
    void testCitizenForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/geography/overview")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/geography/civic-bodies")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/geography/cities")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/geography/wards")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/geography/departments")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/geography/category-mappings")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/geography/wards/validate")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/geography/re-resolve")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MODERATOR cannot mutate or manage civic geography (403 Forbidden)")
    void testModeratorForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/geography/overview")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());

        CreateCivicBodyRequest req = new CreateCivicBodyRequest(
                "Unauthorized Corp",
                CivicBodyType.MUNICIPAL_CORPORATION,
                "Gujarat",
                "Surat",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/admin/geography/civic-bodies")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/geography/wards/validate")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/geography/re-resolve")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN has full access to geography overview and management endpoints")
    void testAdminAllowed() throws Exception {
        mockMvc.perform(get("/api/admin/geography/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/geography/civic-bodies")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/geography/cities")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/geography/wards")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/geography/departments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/geography/category-mappings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/geography/ward-mappings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/geography/wards/validate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
