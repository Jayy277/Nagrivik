package org.nagrivic.modules.push.dto;

import org.nagrivic.modules.push.entity.PushDeviceEntity;
import org.nagrivic.modules.push.model.PlatformType;

import java.time.Instant;
import java.util.UUID;

public record PushDeviceResponse(
    UUID id,
    PlatformType platform,
    String appVersion,
    boolean isActive,
    String maskedToken,
    Instant lastSeenAt,
    Instant createdAt
) {
    public static PushDeviceResponse fromEntity(PushDeviceEntity entity) {
        String token = entity.getDeviceToken();
        String masked = (token != null && token.length() > 10)
                ? token.substring(0, 6) + "..." + token.substring(token.length() - 4)
                : "***";
        return new PushDeviceResponse(
                entity.getId(),
                entity.getPlatform(),
                entity.getAppVersion(),
                entity.isActive(),
                masked,
                entity.getLastSeenAt(),
                entity.getCreatedAt()
        );
    }
}
