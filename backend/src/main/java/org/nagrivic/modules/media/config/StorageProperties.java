package org.nagrivic.modules.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for production-ready object storage foundation.
 * Supports both local development and S3-compatible providers (AWS S3, MinIO, Cloudflare R2).
 */
@Component
@ConfigurationProperties(prefix = "nagrivic.storage")
public class StorageProperties {

    /**
     * Active storage provider: "local" (default) or "s3"
     */
    private String provider = "local";

    private Local local = new Local();
    private S3 s3 = new S3();

    public static class Local {
        private String directory = "build/uploads";

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }
    }

    public static class S3 {
        private String endpoint = "";
        private String region = "us-east-1";
        private String bucket = "nagrivic-media";
        private String accessKey = "";
        private String secretKey = "";
        private boolean pathStyleAccess = false;
        private int connectionTimeoutMs = 10000;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public boolean isPathStyleAccess() {
            return pathStyleAccess;
        }

        public void setPathStyleAccess(boolean pathStyleAccess) {
            this.pathStyleAccess = pathStyleAccess;
        }

        public int getConnectionTimeoutMs() {
            return connectionTimeoutMs;
        }

        public void setConnectionTimeoutMs(int connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
        }
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public S3 getS3() {
        return s3;
    }

    public void setS3(S3 s3) {
        this.s3 = s3;
    }
}
