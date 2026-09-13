package org.nagrivic.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.dto.*;
import org.nagrivic.modules.auth.entity.OtpVerificationEntity;
import org.nagrivic.modules.auth.model.OtpPurpose;
import org.nagrivic.modules.auth.repository.AuthSessionRepository;
import org.nagrivic.modules.auth.repository.OtpVerificationRepository;
import org.nagrivic.modules.auth.service.AuthService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpVerificationRepository otpVerificationRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        authSessionRepository.deleteAll();
        otpVerificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        authSessionRepository.deleteAll();
        otpVerificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==========================================
    // 1. SEND OTP ENDPOINT TESTS
    // ==========================================

    @Test
    void testSendOtpSuccess() throws Exception {
        SendOtpRequest request = new SendOtpRequest("9876543210");

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("+919876543210")))
                .andExpect(jsonPath("$.expiresInSeconds", greaterThan(0)));
    }

    @Test
    void testSendOtpValidationFailure() throws Exception {
        SendOtpRequest request = new SendOtpRequest("invalid-phone");

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")));
    }

    // ==========================================
    // 2. VERIFY OTP ENDPOINT TESTS
    // ==========================================

    @Test
    void testVerifyOtpSuccess() throws Exception {
        // Pre-create OTP record
        String phone = "+919876543210";
        otpVerificationRepository.save(new OtpVerificationEntity(
                phone,
                passwordEncoder.encode("456789"),
                OtpPurpose.LOGIN,
                Instant.now().plus(5, ChronoUnit.MINUTES),
                5
        ));

        VerifyOtpRequest request = new VerifyOtpRequest(phone, "456789");

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(900)))
                .andExpect(jsonPath("$.user.phoneNumber", is(phone)))
                .andExpect(jsonPath("$.user.role", is("CITIZEN")));
    }

    @Test
    void testVerifyOtpInvalidCode() throws Exception {
        String phone = "+919876543210";
        otpVerificationRepository.save(new OtpVerificationEntity(
                phone,
                passwordEncoder.encode("456789"),
                OtpPurpose.LOGIN,
                Instant.now().plus(5, ChronoUnit.MINUTES),
                5
        ));

        VerifyOtpRequest request = new VerifyOtpRequest(phone, "000000");

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Invalid OTP")));
    }

    // ==========================================
    // 3. REFRESH & LOGOUT TESTS
    // ==========================================

    @Test
    void testRefreshTokenSuccessAndRotation() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("+919876543210", "Aarav"));
        otpVerificationRepository.save(new OtpVerificationEntity(
                "+919876543210",
                passwordEncoder.encode("123123"),
                OtpPurpose.LOGIN,
                Instant.now().plus(5, ChronoUnit.MINUTES),
                5
        ));

        AuthResponse authResp = authService.verifyOtp(new VerifyOtpRequest("+919876543210", "123123"));

        RefreshTokenRequest refreshReq = new RefreshTokenRequest(authResp.refreshToken());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", not(equalTo(authResp.refreshToken()))));
    }

    @Test
    void testLogoutEndpoint() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("+919876543210", "Aarav"));
        otpVerificationRepository.save(new OtpVerificationEntity(
                "+919876543210",
                passwordEncoder.encode("123123"),
                OtpPurpose.LOGIN,
                Instant.now().plus(5, ChronoUnit.MINUTES),
                5
        ));

        AuthResponse authResp = authService.verifyOtp(new VerifyOtpRequest("+919876543210", "123123"));

        LogoutRequest logoutReq = new LogoutRequest(authResp.refreshToken());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Logged out successfully")));
    }

    // ==========================================
    // 4. PROTECTED ENDPOINT /api/auth/me
    // ==========================================

    @Test
    void testGetMeUnauthorizedWhenMissingToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    void testGetMeAuthorizedWithValidBearerToken() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("+919876543210", "Aarav"));
        otpVerificationRepository.save(new OtpVerificationEntity(
                "+919876543210",
                passwordEncoder.encode("123123"),
                OtpPurpose.LOGIN,
                Instant.now().plus(5, ChronoUnit.MINUTES),
                5
        ));

        AuthResponse authResp = authService.verifyOtp(new VerifyOtpRequest("+919876543210", "123123"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + authResp.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(user.getId().toString())))
                .andExpect(jsonPath("$.phoneNumber", is("+919876543210")))
                .andExpect(jsonPath("$.role", is("CITIZEN")));
    }

    // ==========================================
    // 5. PUBLIC HEALTH & ISSUE API PRESERVATION
    // ==========================================

    @Test
    void testHealthEndpointPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testIssuesEndpointPreservedPublic() throws Exception {
        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk());
    }
}
