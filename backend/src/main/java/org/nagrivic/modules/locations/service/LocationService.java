package org.nagrivic.modules.locations.service;

import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Transactional
    public LocationEntity createLocation(double latitude, double longitude, BigDecimal accuracyMeters) {
        LocationEntity location = new LocationEntity(latitude, longitude, accuracyMeters);
        return locationRepository.save(location);
    }

    public Optional<LocationEntity> findById(UUID id) {
        return locationRepository.findById(id);
    }
}
