package org.nagrivic.modules.media.config;

import org.nagrivic.modules.media.storage.LocalMediaStorageService;
import org.nagrivic.modules.media.storage.MediaStorageService;
import org.nagrivic.modules.media.storage.S3ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration providing the active MediaStorageService / ObjectStorageService bean
 * based on the configured nagrivic.storage.provider ("local" or "s3").
 */
@Configuration
public class StorageConfiguration {

    private static final Logger log = LoggerFactory.getLogger(StorageConfiguration.class);

    @Bean
    @Primary
    @ConditionalOnMissingBean(MediaStorageService.class)
    public MediaStorageService mediaStorageService(
            StorageProperties storageProperties,
            MediaStorageProperties mediaStorageProperties
    ) {
        String provider = storageProperties.getProvider() != null
                ? storageProperties.getProvider().trim().toLowerCase()
                : "local";

        if ("s3".equalsIgnoreCase(provider)) {
            log.info("Active storage provider: S3-compatible (bucket: '{}', region: '{}', path-style: {})",
                    storageProperties.getS3().getBucket(),
                    storageProperties.getS3().getRegion(),
                    storageProperties.getS3().isPathStyleAccess());
            return new S3ObjectStorageService(storageProperties);
        }

        log.info("Active storage provider: LOCAL (directory: '{}')",
                storageProperties.getLocal().getDirectory());
        return new LocalMediaStorageService(storageProperties);
    }
}
