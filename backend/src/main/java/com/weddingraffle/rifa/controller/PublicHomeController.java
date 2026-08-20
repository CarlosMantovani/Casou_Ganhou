package com.weddingraffle.rifa.controller;

import com.weddingraffle.rifa.dto.FlagRankingResponse;
import com.weddingraffle.rifa.dto.HomeSummaryResponse;
import com.weddingraffle.rifa.service.PublicHomeService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicHomeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicHomeController.class);

    private final PublicHomeService publicHomeService;

    public PublicHomeController(PublicHomeService publicHomeService) {
        this.publicHomeService = publicHomeService;
    }

    @GetMapping("/home-summary")
    public ResponseEntity<HomeSummaryResponse> getSummary() {
        LOGGER.info("Public home summary requested.");
        return ResponseEntity.ok(publicHomeService.getSummary());
    }

    @GetMapping("/flag-ranking")
    public ResponseEntity<List<FlagRankingResponse>> getFlagRanking() {
        LOGGER.info("Public flag ranking requested.");
        return ResponseEntity.ok(publicHomeService.getFlagRanking());
    }
}
