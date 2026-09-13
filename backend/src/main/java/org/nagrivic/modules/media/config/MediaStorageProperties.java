package org.nagrivic.modules.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@ConfigurationProperties(prefix = "nagrivic.media")
public class MediaStorageProperties {

    private int maxFileSizeMb = 10;
    private String localStorageDir = "build/uploads";
    private Set<String> allowedContentTypes = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public int getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public void setMaxFileSizeMb(int maxFileSizeMb) {
        this.maxFileSizeMb = maxFileSizeMb;
    }

    public long getMaxFileSizeBytes() {
        return (long) maxFileSizeMb * 1024 * 1024;
    }

    public String getLocalStorageDir() {
        return localStorageDir;
    }

    public void setLocalStorageDir(String localStorageDir) {
        this.localStorageDir = localStorageDir;
    }

    public Set<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(Set<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }
}
