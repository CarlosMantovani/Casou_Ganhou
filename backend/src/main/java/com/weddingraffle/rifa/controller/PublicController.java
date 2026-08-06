package com.weddingraffle.rifa.controller;

import com.weddingraffle.rifa.dto.HomeSummaryResponse;
import com.weddingraffle.rifa.service.HomeSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicController {

    private final HomeSummaryService homeSummaryService;

    public PublicController(HomeSummaryService homeSummaryService) {
        this.homeSummaryService = homeSummaryService;
    }

    @Operation(summary = "Get public home summary")
    @GetMapping("/home-summary")
    public ResponseEntity<HomeSummaryResponse> getHomeSummary() {
        return ResponseEntity.ok(homeSummaryService.getSummary());
    }
}
