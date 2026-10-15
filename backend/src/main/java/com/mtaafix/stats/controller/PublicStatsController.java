package com.mtaafix.stats.controller;

import com.mtaafix.stats.dto.PublicStatsDto;
import com.mtaafix.stats.service.PublicStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public Stats", description = "Unauthenticated platform statistics (FR-8.1)")
@RestController
@RequestMapping("/api/v1/stats/public")
public class PublicStatsController {

    private final PublicStatsService publicStatsService;

    public PublicStatsController(PublicStatsService publicStatsService) {
        this.publicStatsService = publicStatsService;
    }

    @Operation(summary = "Public aggregate statistics (counts only; no identity or PII)")
    @GetMapping
    public ResponseEntity<PublicStatsDto> stats() {
        return ResponseEntity.ok(publicStatsService.getStats());
    }
}
