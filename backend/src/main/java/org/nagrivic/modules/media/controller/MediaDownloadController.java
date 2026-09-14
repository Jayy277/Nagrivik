package org.nagrivic.modules.media.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.authorities.repository.ResolutionEvidenceRepository;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.media.storage.MediaStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.file.Paths;

/**
 * Controlled media streaming endpoint for verified media files (issue attachments & resolution evidence).
 * Strictly verifies against the database that the requested storageKey exists before streaming from storage.
 */
@RestController
public class MediaDownloadController {

    private static final Logger log = LoggerFactory.getLogger(MediaDownloadController.class);

    private final MediaStorageService mediaStorageService;
    private final MediaRepository mediaRepository;
    private final ResolutionEvidenceRepository resolutionEvidenceRepository;

    public MediaDownloadController(
            MediaStorageService mediaStorageService,
            MediaRepository mediaRepository,
            ResolutionEvidenceRepository resolutionEvidenceRepository
    ) {
        this.mediaStorageService = mediaStorageService;
        this.mediaRepository = mediaRepository;
        this.resolutionEvidenceRepository = resolutionEvidenceRepository;
    }

    /**
     * Streams media binary content for public issue display and resolution verification.
     * Enforces database record verification, path traversal prevention, and safe HTTP headers.
     */
    @GetMapping("/api/media/**")
    public ResponseEntity<Resource> downloadMedia(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = "/api/media/";
        int idx = uri.indexOf(prefix);
        if (idx == -1 || uri.length() <= idx + prefix.length()) {
            throw new ResourceNotFoundException("Media not found");
        }

        String storageKey = uri.substring(idx + prefix.length()).trim();

        // 1. Path traversal defense
        if (storageKey.contains("..") || storageKey.contains("//") || storageKey.contains("\\")) {
            log.warn("Path traversal attempt in media download: {}", storageKey);
            throw new ResourceNotFoundException("Media not found");
        }

        // 2. Database validation: Must exist in media or resolution_evidence tables
        boolean existsInMedia = mediaRepository.existsByStorageKey(storageKey);
        boolean existsInEvidence = resolutionEvidenceRepository.existsByStorageKey(storageKey);

        if (!existsInMedia && !existsInEvidence) {
            log.debug("Rejecting unrecorded media download attempt for key: {}", storageKey);
            throw new ResourceNotFoundException("Media not found");
        }

        // 3. Load stream from storage abstraction
        InputStream stream = mediaStorageService.load(storageKey);

        // 4. Determine content type defensively
        String contentType = determineContentType(storageKey);
        String filename = Paths.get(storageKey).getFileName().toString();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400, immutable")
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(stream));
    }

    private String determineContentType(String key) {
        String lower = key.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
