package org.nagrivic.modules.media.controller;

import org.nagrivic.modules.media.dto.MediaResponse;
import org.nagrivic.modules.media.service.MediaService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/issues/{issueId}/media")
public class IssueMediaController {

    private final MediaService mediaService;

    public IssueMediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * Attaches an image file to the specified issue.
     * Only the authenticated issue reporter can upload media.
     *
     * @param issueId issue UUID from path
     * @param file    multipart image file
     * @return 201 Created with safe media metadata
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> uploadIssueMedia(
            @PathVariable UUID issueId,
            @RequestParam("file") MultipartFile file
    ) {
        MediaResponse response = mediaService.uploadIssueMedia(issueId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves and streams an attached media image file for public issue display.
     *
     * @param issueId issue UUID from path
     * @param mediaId media UUID from path
     * @return image binary resource with correct Content-Type and length
     */
    @GetMapping("/{mediaId}")
    public ResponseEntity<Resource> getIssueMedia(
            @PathVariable UUID issueId,
            @PathVariable UUID mediaId
    ) {
        MediaService.MediaContent content = mediaService.loadMediaContent(issueId, mediaId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.contentLength())
                .body(new InputStreamResource(content.inputStream()));
    }
}
