package org.nagrivic.modules.users.controller;

import jakarta.validation.Valid;
import org.nagrivic.modules.auth.dto.UserAuthSummary;
import org.nagrivic.modules.users.dto.UpdateProfileRequest;
import org.nagrivic.modules.users.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Retrieve the authenticated citizen's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserAuthSummary> getProfile(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserAuthSummary summary = userService.getProfile(userId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Update allowed fields of the authenticated citizen's profile.
     * Identity, role, moderation status, and timestamps are immutable.
     */
    @PatchMapping("/me")
    public ResponseEntity<UserAuthSummary> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserAuthSummary summary = userService.updateProfile(userId, request);
        return ResponseEntity.ok(summary);
    }
}
