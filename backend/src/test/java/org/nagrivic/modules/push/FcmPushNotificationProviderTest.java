package org.nagrivic.modules.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.push.model.PlatformType;
import org.nagrivic.modules.push.provider.NoOpPushNotificationProvider;
import org.nagrivic.modules.push.provider.PushDeliveryResult;
import org.nagrivic.modules.push.provider.PushMessage;
import org.nagrivic.modules.push.provider.PushProviderStatus;
import org.nagrivic.modules.push.provider.fcm.FcmPushNotificationProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FcmPushNotificationProviderTest {

    @Test
    @DisplayName("NoOp provider returns SKIPPED and DISABLED status")
    void testNoOpProvider() {
        NoOpPushNotificationProvider provider = new NoOpPushNotificationProvider();
        assertEquals(PushProviderStatus.DISABLED, provider.getStatus());

        PushMessage msg = new PushMessage("test-token", "Civic Update", "Pothole fixed", Map.of(), PlatformType.ANDROID);
        PushDeliveryResult result = provider.send(msg);

        assertEquals(PushDeliveryResult.Status.SKIPPED, result.status());
        assertFalse(result.isSuccess());
        assertFalse(result.isInvalidToken());
    }

    @Test
    @DisplayName("Blank or empty token returns INVALID_TOKEN")
    void testBlankToken() {
        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        FcmPushNotificationProvider provider = new FcmPushNotificationProvider(mockMessaging);

        PushMessage msg = new PushMessage("   ", "Title", "Body", Map.of(), PlatformType.ANDROID);
        PushDeliveryResult result = provider.send(msg);

        assertEquals(PushDeliveryResult.Status.INVALID_TOKEN, result.status());
        assertTrue(result.isInvalidToken());
        verifyNoInteractions(mockMessaging);
    }

    @Test
    @DisplayName("FCM send success maps message ID and returns SUCCESS")
    void testFcmSendSuccess() throws Exception {
        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        when(mockMessaging.send(any(Message.class))).thenReturn("projects/test/messages/msg-12345");

        FcmPushNotificationProvider provider = new FcmPushNotificationProvider(mockMessaging);
        assertEquals(PushProviderStatus.CONFIGURED, provider.getStatus());

        PushMessage msg = new PushMessage(
                "fcm-valid-token-abcdef",
                "Issue Resolved",
                "Streetlight repairs completed",
                Map.of("issueId", "issue-uuid-1", "deepLink", "nagrivicapp://issue/issue-uuid-1"),
                PlatformType.ANDROID
        );

        PushDeliveryResult result = provider.send(msg);

        assertTrue(result.isSuccess());
        assertEquals("projects/test/messages/msg-12345", result.providerMessageId());
        verify(mockMessaging, times(1)).send(any(Message.class));
    }

    @Test
    @DisplayName("FCM UNREGISTERED error maps to INVALID_TOKEN status")
    void testFcmUnregisteredMapsToInvalidToken() throws Exception {
        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(exception.getMessage()).thenReturn("Requested entity was not found.");
        when(mockMessaging.send(any(Message.class))).thenThrow(exception);

        FcmPushNotificationProvider provider = new FcmPushNotificationProvider(mockMessaging);

        PushMessage msg = new PushMessage("fcm-expired-token", "Title", "Body", Map.of(), PlatformType.IOS);
        PushDeliveryResult result = provider.send(msg);

        assertFalse(result.isSuccess());
        assertTrue(result.isInvalidToken());
        assertEquals("UNREGISTERED", result.errorCode());
    }

    @Test
    @DisplayName("FCM server error maps to FAILED status")
    void testFcmServerErrorMapsToFailed() throws Exception {
        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);
        when(exception.getMessage()).thenReturn("Internal error occurred.");
        when(mockMessaging.send(any(Message.class))).thenThrow(exception);

        FcmPushNotificationProvider provider = new FcmPushNotificationProvider(mockMessaging);

        PushMessage msg = new PushMessage("fcm-token", "Title", "Body", Map.of(), PlatformType.ANDROID);
        PushDeliveryResult result = provider.send(msg);

        assertFalse(result.isSuccess());
        assertFalse(result.isInvalidToken());
        assertEquals("INTERNAL", result.errorCode());
    }
}
