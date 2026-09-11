package org.nagrivic.modules.locations.dto;

import org.nagrivic.modules.locations.entity.LocationEntity;

import java.math.BigDecimal;
import java.util.UUID;

public record LocationResponse(
    UUID id,
    Double latitude,
    Double longitude,
    BigDecimal accuracyMeters
) {
    public static LocationResponse fromEntity(LocationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new LocationResponse(
            entity.getId(),
            entity.getLatitude(),
            entity.getLongitude(),
            entity.getAccuracyMeters()
        );
    }
}
