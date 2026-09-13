package org.nagrivic.modules.civicgeography.service;

import org.nagrivic.modules.civicgeography.dto.CivicAreaResponse;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.locations.entity.LocationEntity;

import java.util.Optional;
import java.util.UUID;

public interface CivicGeographyService {

    Optional<WardEntity> findWardContainingPoint(double latitude, double longitude);

    Optional<CivicAreaResponse> resolvePoint(double latitude, double longitude, UUID categoryId);

    Optional<CivicAreaResponse> resolveLocation(LocationEntity location, UUID categoryId);

    Optional<CivicAreaResponse> enrichIssue(IssueEntity issue);
}
