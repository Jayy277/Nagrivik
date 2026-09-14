package org.nagrivic.modules.push.service;

import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.push.dto.DeactivateDeviceRequest;
import org.nagrivic.modules.push.dto.PushDeviceResponse;
import org.nagrivic.modules.push.dto.RegisterDeviceRequest;
import org.nagrivic.modules.push.entity.PushDeviceEntity;
import org.nagrivic.modules.push.repository.PushDeviceRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PushDeviceServiceImpl implements PushDeviceService {

    private static final Logger log = LoggerFactory.getLogger(PushDeviceServiceImpl.class);

    private final PushDeviceRepository pushDeviceRepository;

    public PushDeviceServiceImpl(PushDeviceRepository pushDeviceRepository) {
        this.pushDeviceRepository = pushDeviceRepository;
    }

    @Override
    @Transactional
    public PushDeviceResponse registerDevice(UserEntity user, RegisterDeviceRequest request) {
        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException("User must be active to register a push device");
        }

        String token = request.token().trim();
        Optional<PushDeviceEntity> existingOpt = pushDeviceRepository.findByDeviceToken(token);

        PushDeviceEntity device;
        if (existingOpt.isPresent()) {
            device = existingOpt.get();
            // Account switching or re-registering
            if (!device.getUser().getId().equals(user.getId())) {
                log.info("Push device token reassigned from user {} to user {}", device.getUser().getId(), user.getId());
                device.setUser(user);
            }
            device.setPlatform(request.platform());
            device.setAppVersion(request.appVersion());
            device.setActive(true);
            device.touch();
        } else {
            device = new PushDeviceEntity(user, token, request.platform(), request.appVersion());
        }

        device = pushDeviceRepository.save(device);
        log.info("Push device registered for user {}: platform={}, isActive={}", user.getId(), device.getPlatform(), device.isActive());
        return PushDeviceResponse.fromEntity(device);
    }

    @Override
    @Transactional
    public void deactivateDevice(UserEntity user, UUID deviceId) {
        if (user == null) {
            throw new IllegalArgumentException("User must be authenticated to deactivate a device");
        }

        PushDeviceEntity device = pushDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Push device not found: " + deviceId));

        if (!device.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("User is not authorized to deactivate this device");
        }

        device.setActive(false);
        pushDeviceRepository.save(device);
        log.info("Push device {} deactivated by owner {}", deviceId, user.getId());
    }

    @Override
    @Transactional
    public void deactivateDeviceByToken(UserEntity user, String token) {
        if (user == null || token == null || token.isBlank()) {
            return;
        }

        Optional<PushDeviceEntity> deviceOpt = pushDeviceRepository.findByDeviceToken(token.trim());
        if (deviceOpt.isPresent()) {
            PushDeviceEntity device = deviceOpt.get();
            if (device.getUser().getId().equals(user.getId())) {
                device.setActive(false);
                pushDeviceRepository.save(device);
                log.info("Push device token deactivated for user {}", user.getId());
            }
        }
    }

    @Override
    @Transactional
    public void markTokenInvalid(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        pushDeviceRepository.findByDeviceToken(token.trim()).ifPresent(device -> {
            device.setActive(false);
            pushDeviceRepository.save(device);
            log.info("Push device token marked inactive due to provider invalidation for user {}", device.getUser().getId());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<PushDeviceEntity> getActiveDevicesForUser(UUID userId) {
        return pushDeviceRepository.findByUser_IdAndIsActiveTrue(userId);
    }
}
