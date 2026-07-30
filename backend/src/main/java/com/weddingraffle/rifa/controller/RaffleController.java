package com.weddingraffle.rifa.controller;

import com.weddingraffle.rifa.dto.RaffleDrawResponse;
import com.weddingraffle.rifa.service.RaffleService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/raffle")
public class RaffleController {

    private final RaffleService raffleService;

    public RaffleController(RaffleService raffleService) {
        this.raffleService = raffleService;
    }

    @Operation(summary = "Draw raffle winner")
    @PostMapping("/draw")
    public ResponseEntity<RaffleDrawResponse> draw() {
        return ResponseEntity.ok(raffleService.draw());
    }

    @Operation(summary = "Get raffle result")
    @GetMapping("/result")
    public ResponseEntity<RaffleDrawResponse> getResult() {
        return ResponseEntity.ok(raffleService.getResult());
    }
}
