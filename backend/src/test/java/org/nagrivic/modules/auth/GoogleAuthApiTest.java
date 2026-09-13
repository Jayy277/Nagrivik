package org.nagrivic.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.dto.*;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.repository.AuthSessionRepository;
import org.nagrivic.modules.auth.repository.UserAuthIdentityRepository;
import org.nagrivic.modules.auth.service.GoogleTokenVerifier;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class GoogleAuthApiTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public GoogleTokenVerifier mockGoogleTokenVerifier() {
            return mock(GoogleTokenVerifier.class);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAuthIdentityRepository userAuthIdentityRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        reset(googleTokenVerifier);
        cleanupDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanupDatabase();
    }

    private void cleanupDatabase() {
        authSessionRepository.deleteAll();
        userAuthIdentityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testGoogleLoginCreatesNewCitizenAndIssuesNagrivicSession() throws Exception {
        String testSub = "google-sub-" + UUID.randomUUID();
        when(googleTokenVerifier.verify("valid-mock-token")).thenReturn(
                new GoogleUserClaims(testSub, "aarav@example.com", true, "Aarav Patel", "https://photo.url")
        );

        GoogleAuthRequest request = new GoogleAuthRequest("valid-mock-token");

        MvcResult result = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.fullName", is("Aarav Patel")))
                .andExpect(jsonPath("$.user.email", is("aarav@example.com")))
                .andExpect(jsonPath("$.user.role", is("CITIZEN")))
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);

        // Verify DB state
        assertTrue(userAuthIdentityRepository.existsByProviderAndProviderSubject("GOOGLE", testSub));
        UserEntity createdUser = userRepository.findById(authResponse.user().id()).orElseThrow();
        assertEquals("Aarav Patel", createdUser.getFullName());
        assertEquals("aarav@example.com", createdUser.getEmail());
        assertEquals("CITIZEN", createdUser.getRole());
        assertTrue(createdUser.isActive());

        // Verify Nagrivic session access with /api/auth/me
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + authResponse.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(authResponse.user().id().toString())))
                .andExpect(jsonPath("$.email", is("aarav@example.com")))
                .andExpect(jsonPath("$.role", is("CITIZEN")));
    }

    @Test
    void testSameGoogleSubjectReturnsSameCitizenAccountWithoutDuplicates() throws Exception {
        String testSub = "google-sub-reusable";
        when(googleTokenVerifier.verify(anyString())).thenReturn(
                new GoogleUserClaims(testSub, "reusable@example.com", true, "Reusable Citizen", null)
        );

        GoogleAuthRequest request = new GoogleAuthRequest("any-valid-token");

        // First login -> creates account
        MvcResult firstResult = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse firstAuth = objectMapper.readValue(firstResult.getResponse().getContentAsString(), AuthResponse.class);

        // Second login -> authenticates existing account
        MvcResult secondResult = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse secondAuth = objectMapper.readValue(secondResult.getResponse().getContentAsString(), AuthResponse.class);

        assertEquals(firstAuth.user().id(), secondAuth.user().id(), "User IDs must match across Google sign-ins");
        assertEquals(1, userRepository.count(), "Exactly one user record must exist");
        assertEquals(1, userAuthIdentityRepository.count(), "Exactly one user auth identity record must exist");
    }

    @Test
    void testRefreshTokenAndLogoutAfterGoogleLogin() throws Exception {
        String testSub = "google-sub-refresh";
        when(googleTokenVerifier.verify("token-for-refresh")).thenReturn(
                new GoogleUserClaims(testSub, "refresh@example.com", true, "Refresh User", null)
        );

        MvcResult loginResult = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleAuthRequest("token-for-refresh"))))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);

        // Refresh token rotation
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(loginResponse.refreshToken());
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", not(equalTo(loginResponse.refreshToken()))))
                .andReturn();

        AuthResponse refreshResponse = objectMapper.readValue(refreshResult.getResponse().getContentAsString(), AuthResponse.class);

        // Logout
        LogoutRequest logoutReq = new LogoutRequest(refreshResponse.refreshToken());
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Logged out successfully")));

        // Attempting to refresh after logout must fail
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshResponse.refreshToken()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testBlankIdTokenRejectedWith400() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest("  ");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void testInvalidGoogleTokenRejectedWith401() throws Exception {
        when(googleTokenVerifier.verify("invalid-token"))
                .thenThrow(AuthException.unauthorized("Invalid, expired, or untrusted Google credential"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleAuthRequest("invalid-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }
}
