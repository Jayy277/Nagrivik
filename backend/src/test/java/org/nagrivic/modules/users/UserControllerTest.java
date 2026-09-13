package org.nagrivic.modules.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.users.dto.UpdateProfileRequest;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class UserControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private UserEntity citizen;
    private String citizenToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        citizen = new UserEntity("Initial Name", "citizen@example.com", "https://example.com/photo.jpg");
        citizen.setPhoneNumber("+919876543210");
        citizen.setRole("CITIZEN");
        citizen.setActive(true);
        citizen = userRepository.save(citizen);

        citizenToken = jwtService.generateAccessToken(citizen.getId(), citizen.getRole());
    }

    @Test
    void getProfile_authenticated_returnsSafeProfileSummary() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(citizen.getId().toString())))
                .andExpect(jsonPath("$.fullName", is("Initial Name")))
                .andExpect(jsonPath("$.email", is("citizen@example.com")))
                .andExpect(jsonPath("$.phoneNumber", is("+919876543210")))
                .andExpect(jsonPath("$.role", is("CITIZEN")))
                .andExpect(jsonPath("$.profilePictureUrl", is("https://example.com/photo.jpg")));
    }

    @Test
    void getProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_validFullName_persistsAndReturnsUpdatedUser() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Aarav Patel");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(citizen.getId().toString())))
                .andExpect(jsonPath("$.fullName", is("Aarav Patel")))
                .andExpect(jsonPath("$.email", is("citizen@example.com")));

        UserEntity updated = userRepository.findById(citizen.getId()).orElseThrow();
        assertThat(updated.getFullName()).isEqualTo("Aarav Patel");
        assertThat(updated.getRole()).isEqualTo("CITIZEN");
        assertThat(updated.getEmail()).isEqualTo("citizen@example.com");
    }

    @Test
    void updateProfile_gujaratiUnicodeName_persistsCorrectly() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("રાજ પટેલ");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("રાજ પટેલ")));

        UserEntity updated = userRepository.findById(citizen.getId()).orElseThrow();
        assertThat(updated.getFullName()).isEqualTo("રાજ પટેલ");
    }

    @Test
    void updateProfile_blankFullName_returns400() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("   ");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_controlCharacters_returns400() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Aarav\u0000Patel");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_inactiveUser_returns403() throws Exception {
        citizen.setActive(false);
        userRepository.save(citizen);

        UpdateProfileRequest request = new UpdateProfileRequest("Valid Name");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
