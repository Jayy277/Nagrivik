package org.nagrivic.modules.supports.controller;

import org.nagrivic.modules.supports.service.SupportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/issues/{issueId}/support")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    /**
     * Supports a civic issue as the authenticated citizen.
     *
     * @param issueId target issue UUID
     * @return 201 Created
     */
    @PostMapping
    public ResponseEntity<Void> addSupport(@PathVariable UUID issueId) {
        supportService.addSupport(issueId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Removes the authenticated citizen's support from a civic issue.
     *
     * @param issueId target issue UUID
     * @return 204 No Content
     */
    @DeleteMapping
    public ResponseEntity<Void> removeSupport(@PathVariable UUID issueId) {
        supportService.removeSupport(issueId);
        return ResponseEntity.noContent().build();
    }
}
