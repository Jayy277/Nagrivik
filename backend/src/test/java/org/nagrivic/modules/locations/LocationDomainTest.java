package org.nagrivic.modules.locations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import org.nagrivic.modules.locations.dto.LocationResponse;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.repository.LocationRepository;
import org.nagrivic.modules.locations.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LocationDomainTest {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private LocationService locationService;

    @BeforeEach
    void setUp() {
        locationRepository.deleteAll();
    }

    @Test
    void shouldCreateAndPersistLocationWithValidCoordinates() {
        // Ahmedabad Civic Centre coordinates: Latitude 23.0225, Longitude 72.5714
        double latitude = 23.0225;
        double longitude = 72.5714;
        BigDecimal accuracyMeters = new BigDecimal("4.50");

        LocationEntity location = locationService.createLocation(latitude, longitude, accuracyMeters);

        assertNotNull(location.getId());
        assertEquals(latitude, location.getLatitude(), 0.0001);
        assertEquals(longitude, location.getLongitude(), 0.0001);
        assertEquals(accuracyMeters, location.getAccuracyMeters());
        assertNotNull(location.getCreatedAt());
        assertNotNull(location.getUpdatedAt());

        // Verify PostGIS geometry properties
        Point point = location.getLocationPoint();
        assertNotNull(point);
        assertEquals(4326, point.getSRID(), "SRID must be 4326 (WGS 84)");
        assertEquals(longitude, point.getX(), 0.0001, "Point X coordinate must represent longitude");
        assertEquals(latitude, point.getY(), 0.0001, "Point Y coordinate must represent latitude");
    }

    @Test
    void shouldFindLocationById() {
        LocationEntity saved = locationService.createLocation(19.0760, 72.8777, new BigDecimal("10.00")); // Mumbai

        Optional<LocationEntity> found = locationService.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(19.0760, found.get().getLatitude(), 0.0001);
        assertEquals(72.8777, found.get().getLongitude(), 0.0001);
    }

    @Test
    void shouldRejectLatitudeBelowMinus90() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            locationService.createLocation(-90.001, 72.5714, null);
        });
        assertTrue(ex.getMessage().contains("Latitude must be between -90 and 90 degrees"));
    }

    @Test
    void shouldRejectLatitudeAbove90() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            locationService.createLocation(90.001, 72.5714, null);
        });
        assertTrue(ex.getMessage().contains("Latitude must be between -90 and 90 degrees"));
    }

    @Test
    void shouldRejectLongitudeBelowMinus180() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            locationService.createLocation(23.0225, -180.001, null);
        });
        assertTrue(ex.getMessage().contains("Longitude must be between -180 and 180 degrees"));
    }

    @Test
    void shouldRejectLongitudeAbove180() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            locationService.createLocation(23.0225, 180.001, null);
        });
        assertTrue(ex.getMessage().contains("Longitude must be between -180 and 180 degrees"));
    }

    @Test
    void shouldRejectNegativeAccuracyMeters() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            locationService.createLocation(23.0225, 72.5714, new BigDecimal("-1.00"));
        });
        assertTrue(ex.getMessage().contains("Accuracy meters cannot be negative"));
    }

    @Test
    void shouldAllowBoundaryCoordinates() {
        // Valid exact boundaries: -90, +90 for latitude; -180, +180 for longitude
        LocationEntity southWest = locationService.createLocation(-90.0, -180.0, null);
        assertEquals(-90.0, southWest.getLatitude(), 0.0001);
        assertEquals(-180.0, southWest.getLongitude(), 0.0001);

        LocationEntity northEast = locationService.createLocation(90.0, 180.0, BigDecimal.ZERO);
        assertEquals(90.0, northEast.getLatitude(), 0.0001);
        assertEquals(180.0, northEast.getLongitude(), 0.0001);
    }

    @Test
    void shouldMapLocationResponseDtoWithoutExposingInternalGeometry() {
        LocationEntity location = locationService.createLocation(28.6139, 77.2090, new BigDecimal("8.25")); // New Delhi
        LocationResponse response = LocationResponse.fromEntity(location);

        assertNotNull(response);
        assertEquals(location.getId(), response.id());
        assertEquals(28.6139, response.latitude(), 0.0001);
        assertEquals(77.2090, response.longitude(), 0.0001);
        assertEquals(new BigDecimal("8.25"), response.accuracyMeters());
    }
}
