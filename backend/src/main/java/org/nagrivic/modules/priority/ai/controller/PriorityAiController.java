package org.nagrivic.modules.priority.ai.controller;

import org.nagrivic.modules.auth.service.CurrentUserService;
import org.nagrivic.modules.priority.ai.dto.AiPriorityRecommendationResponse;
import org.nagrivic.modules.priority.ai.service.PriorityAiService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/issues/{issueId}/priority")
public class PriorityAiController {

    private final PriorityAiService priorityAiService;
    private final CurrentUserService currentUserService;

    public PriorityAiController(
            PriorityAiService priorityAiService,
            CurrentUserService currentUserService
    ) {
        this.priorityAiService = priorityAiService;
        this.currentUserService = currentUserService;
    }

    /**
     * Inspect AI priority recommendation for an issue.
     * Accessible by the reporter, or authorized authority officers, moderators, and admins.
     */
    @GetMapping("/ai-recommendation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiPriorityRecommendationResponse> getRecommendation(
            @PathVariable UUID issueId
    ) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        String role = currentUser != null ? currentUser.getRole() : null;
        UUID userId = currentUser != null ? currentUser.getId() : null;

        AiPriorityRecommendationResponse response = priorityAiService.getRecommendation(issueId, userId, role);
        return ResponseEntity.ok(response);
    }

    /**
     * Trigger an AI priority assessment or reassessment for an issue.
     * Privileged operation restricted to officers, moderators, and admins.
     */
    @PostMapping("/ai-assess")
    @PreAuthorize("hasAnyRole('OFFICER', 'AUTHORITY', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<AiPriorityRecommendationResponse> assessPriority(
            @PathVariable UUID issueId
    ) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        String role = currentUser != null ? currentUser.getRole() : null;
        UUID userId = currentUser != null ? currentUser.getId() : null;

        AiPriorityRecommendationResponse response = priorityAiService.assessPriority(issueId, userId, role);
        return ResponseEntity.ok(response);
    }
}
