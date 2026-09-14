package org.nagrivic.modules.push.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Firebase Cloud Messaging (FCM).
 */
@Component
@ConfigurationProperties(prefix = "nagrivic.push.fcm")
public class PushProperties {

    private boolean enabled = false;
    private String projectId;
    private String clientEmail;
    private String privateKey;
    private String credentialsPath;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getCredentialsPath() {
        return credentialsPath;
    }

    public void setCredentialsPath(String credentialsPath) {
        this.credentialsPath = credentialsPath;
    }
}
