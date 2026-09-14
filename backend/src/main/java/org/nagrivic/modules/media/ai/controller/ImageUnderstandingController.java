package org.nagrivic.modules.media.ai.controller;

import org.nagrivic.modules.media.ai.dto.ImageAiAnalysisResponse;
import org.nagrivic.modules.media.ai.service.ImageUnderstandingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/issues/{issueId}/media/{mediaId}")
public class ImageUnderstandingController {

    private final ImageUnderstandingService imageUnderstandingService;

    public ImageUnderstandingController(ImageUnderstandingService imageUnderstandingService) {
        this.imageUnderstandingService = imageUnderstandingService;
    }

    /**
     * Triggers AI-assisted image understanding for an attached issue photo.
     * Accessible by the issue reporter, municipal officers, administrators, or moderators.
     */
    @PostMapping("/analyze")
    public ResponseEntity<ImageAiAnalysisResponse> analyzeMedia(
            @PathVariable UUID issueId,
            @PathVariable UUID mediaId
    ) {
        ImageAiAnalysisResponse response = imageUnderstandingService.analyzeMedia(issueId, mediaId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the persisted AI image understanding analysis for an attached photo.
     * Accessible by the issue reporter, municipal officers, administrators, or moderators.
     */
    @GetMapping("/analysis")
    public ResponseEntity<ImageAiAnalysisResponse> getAnalysis(
            @PathVariable UUID issueId,
            @PathVariable UUID mediaId
    ) {
        ImageAiAnalysisResponse response = imageUnderstandingService.getAnalysis(issueId, mediaId);
        return ResponseEntity.ok(response);
    }
}
