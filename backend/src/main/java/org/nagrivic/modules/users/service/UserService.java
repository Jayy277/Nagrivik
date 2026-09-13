package org.nagrivic.modules.users.service;

import org.nagrivic.modules.auth.dto.UserAuthSummary;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.users.dto.UpdateProfileRequest;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserAuthSummary getProfile(UUID userId) {
        if (userId == null) {
            throw AuthException.unauthorized("Authentication required");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> AuthException.unauthorized("User not found"));

        if (!user.isActive()) {
            throw AuthException.forbidden("User account is inactive");
        }

        return toUserSummary(user);
    }

    @Transactional
    public UserAuthSummary updateProfile(UUID userId, UpdateProfileRequest request) {
        if (userId == null) {
            throw AuthException.unauthorized("Authentication required");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> AuthException.unauthorized("User not found"));

        if (!user.isActive()) {
            throw AuthException.forbidden("User account is inactive");
        }

        if (request == null || request.fullName() == null) {
            throw AuthException.badRequest("Full name cannot be blank");
        }

        String trimmedName = request.fullName().trim();
        if (trimmedName.length() < 2 || trimmedName.length() > 100) {
            throw AuthException.badRequest("Full name must be between 2 and 100 characters");
        }

        // Reject non-printable control characters while supporting all Unicode / Indian scripts
        if (trimmedName.chars().anyMatch(Character::isISOControl)) {
            throw AuthException.badRequest("Full name contains invalid control characters");
        }

        user.setFullName(trimmedName);
        UserEntity saved = userRepository.save(user);

        log.info("Citizen profile updated successfully for userId: {}", userId);
        return toUserSummary(saved);
    }

    private UserAuthSummary toUserSummary(UserEntity user) {
        return new UserAuthSummary(
                user.getId(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getRole(),
                user.getFullName(),
                user.getProfilePictureUrl()
        );
    }
}
