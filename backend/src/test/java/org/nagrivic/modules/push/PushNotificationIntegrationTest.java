package org.nagrivic.modules.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.notifications.dto.UpdateNotificationPreferenceRequest;
import org.nagrivic.modules.notifications.entity.NotificationEntity;
import org.nagrivic.modules.notifications.entity.NotificationPreferenceEntity;
import org.nagrivic.modules.notifications.event.NotificationCreatedEvent;
import org.nagrivic.modules.notifications.model.NotificationType;
import org.nagrivic.modules.notifications.repository.NotificationPreferenceRepository;
import org.nagrivic.modules.notifications.repository.NotificationRepository;
import org.nagrivic.modules.notifications.service.NotificationService;
import org.nagrivic.modules.push.dto.DeactivateDeviceRequest;
import org.nagrivic.modules.push.dto.RegisterDeviceRequest;
import org.nagrivic.modules.push.entity.PushDeliveryEntity;
import org.nagrivic.modules.push.entity.PushDeviceEntity;
import org.nagrivic.modules.push.model.PlatformType;
import org.nagrivic.modules.push.provider.PushDeliveryResult;
import org.nagrivic.modules.push.provider.PushMessage;
import org.nagrivic.modules.push.provider.PushNotificationProvider;
import org.nagrivic.modules.push.repository.PushDeliveryRepository;
import org.nagrivic.modules.push.repository.PushDeviceRepository;
import org.nagrivic.modules.push.service.PushDeviceService;
import org.nagrivic.modules.push.service.PushNotificationDispatcher;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PushNotificationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PushDeviceRepository pushDeviceRepository;

    @Autowired
    private PushDeliveryRepository pushDeliveryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private PushDeviceService pushDeviceService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationService locationService;

    private MockMvc mockMvc;
    private UserEntity citizen;
    private String citizenToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        cleanDatabase();

        citizen = userRepository.save(new UserEntity("+919988776655", "Citizen Push Tester"));
        citizenToken = jwtService.generateAccessToken(citizen.getId(), citizen.getRole());
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        pushDeliveryRepository.deleteAll();
        pushDeviceRepository.deleteAll();
        notificationRepository.deleteAll();
        preferenceRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Unauthenticated request to register push device returns 401 Unauthorized")
    void testRegisterDeviceUnauthenticatedFails() throws Exception {
        RegisterDeviceRequest request = new RegisterDeviceRequest(
                "unauth-token-123",
                PlatformType.ANDROID,
                "1.0.0"
        );

        mockMvc.perform(post("/api/push/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authenticated user can register device token and receive masked token metadata")
    void testRegisterDeviceAuthenticatedSuccess() throws Exception {
        RegisterDeviceRequest request = new RegisterDeviceRequest(
                "fcm-sample-device-token-987654321",
                PlatformType.ANDROID,
                "2.1.0"
        );

        mockMvc.perform(post("/api/push/devices")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.platform").value("ANDROID"))
                .andExpect(jsonPath("$.appVersion").value("2.1.0"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.maskedToken").value("fcm-sa...4321"));

        assertEquals(1, pushDeviceRepository.count());
    }

    @Test
    @DisplayName("User can deactivate device by ID")
    void testDeactivateDeviceById() throws Exception {
        PushDeviceEntity device = pushDeviceRepository.save(
                new PushDeviceEntity(citizen, "test-token-to-deactivate", PlatformType.IOS, "1.0.0")
        );
        assertTrue(device.isActive());

        mockMvc.perform(delete("/api/push/devices/" + device.getId())
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk());

        PushDeviceEntity updated = pushDeviceRepository.findById(device.getId()).orElseThrow();
        assertFalse(updated.isActive());
    }

    @Test
    @DisplayName("User can deactivate device by token (e.g. on logout)")
    void testDeactivateDeviceByToken() throws Exception {
        String token = "logout-deactivate-token-111";
        pushDeviceRepository.save(new PushDeviceEntity(citizen, token, PlatformType.ANDROID, "1.0.0"));

        DeactivateDeviceRequest request = new DeactivateDeviceRequest(token);

        mockMvc.perform(post("/api/push/devices/deactivate")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        PushDeviceEntity updated = pushDeviceRepository.findByDeviceToken(token).orElseThrow();
        assertFalse(updated.isActive());
    }

    @Test
    @DisplayName("Push delivery skips when user push preference is disabled")
    void testPushSkippedWhenPreferenceDisabled() {
        // Register active device
        pushDeviceRepository.save(new PushDeviceEntity(citizen, "token-pref-disabled", PlatformType.ANDROID, "1.0.0"));

        // Disable push in preferences
        NotificationPreferenceEntity pref = new NotificationPreferenceEntity(citizen);
        pref.setPushEnabled(false);
        preferenceRepository.save(pref);

        PushNotificationProvider mockProvider = mock(PushNotificationProvider.class);
        PushNotificationDispatcher dispatcher = new PushNotificationDispatcher(
                mockProvider,
                pushDeviceService,
                pushDeliveryRepository,
                preferenceRepository,
                userRepository,
                notificationRepository
        );

        NotificationEntity notif = new NotificationEntity(
                citizen,
                NotificationType.ISSUE_STATUS_CHANGED,
                "Status Updated",
                "Your reported issue is in progress",
                null,
                "event-1",
                Map.of()
        );
        notif = notificationRepository.save(notif);

        dispatcher.onNotificationCreated(NotificationCreatedEvent.fromEntity(notif));

        // Provider send is never called because preference is disabled
        verifyNoInteractions(mockProvider);
        assertEquals(0, pushDeliveryRepository.count());
    }

    @Test
    @DisplayName("Push delivery executes and records audit log when push preference is enabled")
    void testPushDeliveredWhenPreferenceEnabled() {
        // Register active device
        PushDeviceEntity device = pushDeviceRepository.save(
                new PushDeviceEntity(citizen, "fcm-valid-push-token-12345", PlatformType.ANDROID, "1.0.0")
        );

        // Enable push in preferences
        NotificationPreferenceEntity pref = new NotificationPreferenceEntity(citizen);
        pref.setPushEnabled(true);
        preferenceRepository.save(pref);

        PushNotificationProvider mockProvider = mock(PushNotificationProvider.class);
        when(mockProvider.send(any(PushMessage.class))).thenReturn(PushDeliveryResult.success("fcm-msg-id-123"));

        PushNotificationDispatcher dispatcher = new PushNotificationDispatcher(
                mockProvider,
                pushDeviceService,
                pushDeliveryRepository,
                preferenceRepository,
                userRepository,
                notificationRepository
        );

        NotificationEntity notif = new NotificationEntity(
                citizen,
                NotificationType.ISSUE_RESOLVED,
                "Issue Resolved",
                "Civic authority marked issue resolved",
                null,
                "event-resolved-1",
                Map.of()
        );
        notif = notificationRepository.save(notif);

        dispatcher.onNotificationCreated(NotificationCreatedEvent.fromEntity(notif));

        verify(mockProvider, times(1)).send(any(PushMessage.class));

        List<PushDeliveryEntity> deliveries = pushDeliveryRepository.findByNotification_Id(notif.getId());
        assertEquals(1, deliveries.size());
        assertEquals("SUCCESS", deliveries.get(0).getStatus());
        assertEquals("fcm-msg-id-123", deliveries.get(0).getProviderMessageId());
        assertEquals(device.getId(), deliveries.get(0).getDevice().getId());
    }
}
