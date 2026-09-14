package org.nagrivic.modules.push;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.push.dto.PushDeviceResponse;
import org.nagrivic.modules.push.dto.RegisterDeviceRequest;
import org.nagrivic.modules.push.entity.PushDeviceEntity;
import org.nagrivic.modules.push.model.PlatformType;
import org.nagrivic.modules.push.repository.PushDeviceRepository;
import org.nagrivic.modules.push.service.PushDeviceService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PushDeviceServiceTest {

    @Autowired
    private PushDeviceService pushDeviceService;

    @Autowired
    private PushDeviceRepository pushDeviceRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity userA;
    private UserEntity userB;

    @BeforeEach
    void setUp() {
        pushDeviceRepository.deleteAll();
        userRepository.deleteAll();

        userA = userRepository.save(new UserEntity("+919000000001", "User Alpha"));
        userB = userRepository.save(new UserEntity("+919000000002", "User Beta"));
    }

    @AfterEach
    void tearDown() {
        pushDeviceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Authenticated user can register a new push device")
    void testRegisterNewDevice() {
        RegisterDeviceRequest request = new RegisterDeviceRequest(
                "fcm-token-alpha-1234567890",
                PlatformType.ANDROID,
                "1.0.0"
        );

        PushDeviceResponse response = pushDeviceService.registerDevice(userA, request);

        assertNotNull(response.id());
        assertEquals(PlatformType.ANDROID, response.platform());
        assertEquals("1.0.0", response.appVersion());
        assertTrue(response.isActive());
        assertTrue(response.maskedToken().startsWith("fcm-to..."));

        List<PushDeviceEntity> activeDevices = pushDeviceService.getActiveDevicesForUser(userA.getId());
        assertEquals(1, activeDevices.size());
        assertEquals("fcm-token-alpha-1234567890", activeDevices.get(0).getDeviceToken());
    }

    @Test
    @DisplayName("Registering an existing token re-activates and updates metadata idempotently")
    void testReRegisterExistingToken() {
        RegisterDeviceRequest request1 = new RegisterDeviceRequest("token-dup-123456", PlatformType.IOS, "1.0.0");
        PushDeviceResponse resp1 = pushDeviceService.registerDevice(userA, request1);

        // Deactivate
        pushDeviceService.deactivateDevice(userA, resp1.id());
        assertFalse(pushDeviceRepository.findById(resp1.id()).orElseThrow().isActive());

        // Re-register with newer app version
        RegisterDeviceRequest request2 = new RegisterDeviceRequest("token-dup-123456", PlatformType.IOS, "1.0.1");
        PushDeviceResponse resp2 = pushDeviceService.registerDevice(userA, request2);

        assertEquals(resp1.id(), resp2.id());
        assertTrue(resp2.isActive());
        assertEquals("1.0.1", resp2.appVersion());
        assertEquals(1, pushDeviceRepository.count());
    }

    @Test
    @DisplayName("Token is reassigned cleanly when device account switches")
    void testAccountSwitchingTokenReassignment() {
        String sharedDeviceToken = "shared-phone-device-token-123";

        // User A logs in on device
        pushDeviceService.registerDevice(userA, new RegisterDeviceRequest(sharedDeviceToken, PlatformType.ANDROID, "1.0.0"));
        assertEquals(1, pushDeviceService.getActiveDevicesForUser(userA.getId()).size());
        assertEquals(0, pushDeviceService.getActiveDevicesForUser(userB.getId()).size());

        // User B logs in on same device
        pushDeviceService.registerDevice(userB, new RegisterDeviceRequest(sharedDeviceToken, PlatformType.ANDROID, "1.0.0"));

        // User A no longer has this active device; User B owns it now
        assertEquals(0, pushDeviceService.getActiveDevicesForUser(userA.getId()).size());
        assertEquals(1, pushDeviceService.getActiveDevicesForUser(userB.getId()).size());
    }

    @Test
    @DisplayName("User cannot deactivate another user's device registration")
    void testCannotDeactivateOthersDevice() {
        PushDeviceResponse resp = pushDeviceService.registerDevice(
                userA,
                new RegisterDeviceRequest("user-a-device-token-999", PlatformType.ANDROID, "1.0.0")
        );

        assertThrows(AccessDeniedException.class, () -> {
            pushDeviceService.deactivateDevice(userB, resp.id());
        });

        // Device remains active for User A
        assertTrue(pushDeviceRepository.findById(resp.id()).orElseThrow().isActive());
    }

    @Test
    @DisplayName("Deactivate by token deactivates only if user owns the device")
    void testDeactivateByToken() {
        String token = "user-a-logout-token-123";
        pushDeviceService.registerDevice(userA, new RegisterDeviceRequest(token, PlatformType.IOS, "1.0.0"));
        assertTrue(pushDeviceService.getActiveDevicesForUser(userA.getId()).get(0).isActive());

        // Attempt deactivation as User B (should have no effect)
        pushDeviceService.deactivateDeviceByToken(userB, token);
        assertTrue(pushDeviceService.getActiveDevicesForUser(userA.getId()).get(0).isActive());

        // Deactivate as User A
        pushDeviceService.deactivateDeviceByToken(userA, token);
        assertEquals(0, pushDeviceService.getActiveDevicesForUser(userA.getId()).size());
    }

    @Test
    @DisplayName("Provider invalid token mark invalid sets isActive to false")
    void testMarkTokenInvalid() {
        String badToken = "invalid-unregistered-fcm-token";
        pushDeviceService.registerDevice(userA, new RegisterDeviceRequest(badToken, PlatformType.ANDROID, "1.0.0"));
        assertEquals(1, pushDeviceService.getActiveDevicesForUser(userA.getId()).size());

        pushDeviceService.markTokenInvalid(badToken);
        assertEquals(0, pushDeviceService.getActiveDevicesForUser(userA.getId()).size());
    }
}
