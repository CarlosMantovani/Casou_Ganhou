package com.weddingraffle.rifa.controller;

import com.weddingraffle.rifa.dto.FlagRankingResponse;
import com.weddingraffle.rifa.dto.HomeSummaryResponse;
import com.weddingraffle.rifa.service.PublicHomeService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicHomeController {

    private final PublicHomeService publicHomeService;

    public PublicHomeController(PublicHomeService publicHomeService) {
        this.publicHomeService = publicHomeService;
    }

    @GetMapping("/home-summary")
    public ResponseEntity<HomeSummaryResponse> getSummary() {
        return ResponseEntity.ok(publicHomeService.getSummary());
    }

    @GetMapping("/flag-ranking")
    public ResponseEntity<List<FlagRankingResponse>> getFlagRanking() {
        return ResponseEntity.ok(publicHomeService.getFlagRanking());
    }
}
