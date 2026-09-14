package org.nagrivic.modules.push.controller;

import jakarta.validation.Valid;
import org.nagrivic.modules.auth.service.CurrentUserService;
import org.nagrivic.modules.push.dto.DeactivateDeviceRequest;
import org.nagrivic.modules.push.dto.PushDeviceResponse;
import org.nagrivic.modules.push.dto.RegisterDeviceRequest;
import org.nagrivic.modules.push.service.PushDeviceService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/push/devices")
public class PushDeviceController {

    private final PushDeviceService pushDeviceService;
    private final CurrentUserService currentUserService;

    public PushDeviceController(PushDeviceService pushDeviceService, CurrentUserService currentUserService) {
        this.pushDeviceService = pushDeviceService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<PushDeviceResponse> registerDevice(@Valid @RequestBody RegisterDeviceRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        PushDeviceResponse response = pushDeviceService.registerDevice(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Map<String, String>> deactivateDevice(@PathVariable UUID deviceId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        pushDeviceService.deactivateDevice(currentUser, deviceId);
        return ResponseEntity.ok(Map.of("message", "Device registration deactivated successfully"));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Map<String, String>> deactivateByToken(@Valid @RequestBody DeactivateDeviceRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        pushDeviceService.deactivateDeviceByToken(currentUser, request.token());
        return ResponseEntity.ok(Map.of("message", "Device token deactivated successfully"));
    }
}
