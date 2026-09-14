package org.nagrivic.modules.push.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.nagrivic.modules.push.provider.NoOpPushNotificationProvider;
import org.nagrivic.modules.push.provider.PushNotificationProvider;
import org.nagrivic.modules.push.provider.fcm.FcmPushNotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class PushConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PushConfiguration.class);

    @Bean
    @Primary
    public PushNotificationProvider pushNotificationProvider(PushProperties properties) {
        if (!properties.isEnabled()) {
            log.info("Push notification provider is DISABLED (nagrivic.push.fcm.enabled=false). Using NoOpPushNotificationProvider.");
            return new NoOpPushNotificationProvider();
        }

        try {
            FirebaseApp app = initFirebaseApp(properties);
            FirebaseMessaging messaging = FirebaseMessaging.getInstance(app);
            log.info("Push notification provider successfully initialized with FCM (Project ID: {}).", properties.getProjectId());
            return new FcmPushNotificationProvider(messaging);
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Admin SDK for FCM: {}. Falling back to NoOpPushNotificationProvider.", e.getMessage(), e);
            return new NoOpPushNotificationProvider();
        }
    }

    private FirebaseApp initFirebaseApp(PushProperties properties) throws Exception {
        // Return existing default app if already initialized
        for (FirebaseApp app : FirebaseApp.getApps()) {
            if (app.getName().equals(FirebaseApp.DEFAULT_APP_NAME)) {
                return app;
            }
        }

        GoogleCredentials credentials;

        if (properties.getCredentialsPath() != null && !properties.getCredentialsPath().isBlank()) {
            try (InputStream is = new FileInputStream(properties.getCredentialsPath())) {
                credentials = GoogleCredentials.fromStream(is);
            }
        } else if (properties.getPrivateKey() != null && !properties.getPrivateKey().isBlank()
                && properties.getClientEmail() != null && !properties.getClientEmail().isBlank()) {
            // Build minimal service account JSON in memory without writing secrets to disk
            String privateKey = properties.getPrivateKey().replace("\\n", "\n");
            String serviceAccountJson = String.format(
                    "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"%s\",\n" +
                    "  \"client_email\": \"%s\",\n" +
                    "  \"private_key\": \"%s\"\n" +
                    "}",
                    properties.getProjectId() != null ? properties.getProjectId() : "",
                    properties.getClientEmail(),
                    privateKey.replace("\"", "\\\"").replace("\n", "\\n")
            );
            try (InputStream is = new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))) {
                credentials = GoogleCredentials.fromStream(is);
            }
        } else {
            // Default application credentials (environment / GCP runtime)
            credentials = GoogleCredentials.getApplicationDefault();
        }

        FirebaseOptions.Builder builder = FirebaseOptions.builder().setCredentials(credentials);
        if (properties.getProjectId() != null && !properties.getProjectId().isBlank()) {
            builder.setProjectId(properties.getProjectId());
        }

        return FirebaseApp.initializeApp(builder.build());
    }
}
