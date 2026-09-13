package org.nagrivic.modules.civicgeography.repository;

import org.nagrivic.modules.civicgeography.entity.WardEntity;

import java.util.Optional;

public interface WardSpatialRepository {
    Optional<WardEntity> findWardContainingPoint(double latitude, double longitude);
}
