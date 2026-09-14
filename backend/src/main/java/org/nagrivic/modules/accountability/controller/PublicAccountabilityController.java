package org.nagrivic.modules.accountability.controller;

import org.nagrivic.modules.accountability.dto.AccountabilityFilter;
import org.nagrivic.modules.accountability.dto.PublicAccountabilityResponse;
import org.nagrivic.modules.accountability.service.PublicAccountabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/accountability")
public class PublicAccountabilityController {

    private final PublicAccountabilityService accountabilityService;

    public PublicAccountabilityController(PublicAccountabilityService accountabilityService) {
        this.accountabilityService = accountabilityService;
    }

    @GetMapping
    public ResponseEntity<PublicAccountabilityResponse> getAccountability(
            @RequestParam(name = "cityId", required = false) UUID cityId,
            @RequestParam(name = "wardId", required = false) UUID wardId,
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @RequestParam(name = "range", required = false, defaultValue = "30d") String range
    ) {
        AccountabilityFilter filter = new AccountabilityFilter(cityId, wardId, categoryId, range);
        PublicAccountabilityResponse response = accountabilityService.getPublicAccountability(filter);
        return ResponseEntity.ok(response);
    }
}
