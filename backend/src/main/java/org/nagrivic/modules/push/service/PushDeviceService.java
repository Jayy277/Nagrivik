package org.nagrivic.modules.push.service;

import org.nagrivic.modules.push.dto.DeactivateDeviceRequest;
import org.nagrivic.modules.push.dto.PushDeviceResponse;
import org.nagrivic.modules.push.dto.RegisterDeviceRequest;
import org.nagrivic.modules.push.entity.PushDeviceEntity;
import org.nagrivic.modules.users.entity.UserEntity;

import java.util.List;
import java.util.UUID;

public interface PushDeviceService {

    /**
     * Registers or updates a device token for the authenticated user.
     * Safely handles token re-assignment if an account switched on the same device.
     */
    PushDeviceResponse registerDevice(UserEntity user, RegisterDeviceRequest request);

    /**
     * Deactivates a device registration by device ID.
     * Verifies that the device belongs to the authenticated user.
     */
    void deactivateDevice(UserEntity user, UUID deviceId);

    /**
     * Deactivates a device registration by device token for the authenticated user (e.g. on logout).
     */
    void deactivateDeviceByToken(UserEntity user, String token);

    /**
     * Deactivates a device registration when the push provider definitively indicates the token is invalid.
     */
    void markTokenInvalid(String token);

    /**
     * Retrieves all active device registrations for the given user.
     */
    List<PushDeviceEntity> getActiveDevicesForUser(UUID userId);
}
