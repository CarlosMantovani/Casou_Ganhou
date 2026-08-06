package com.weddingraffle.rifa.controller;

import com.weddingraffle.rifa.dto.RaffleConfigResponse;
import com.weddingraffle.rifa.dto.RaffleConfigScheduledDrawAtRequest;
import com.weddingraffle.rifa.dto.RaffleConfigUnitPriceRequest;
import com.weddingraffle.rifa.service.RaffleConfigService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/raffle-config")
public class RaffleConfigController {

    private final RaffleConfigService raffleConfigService;

    public RaffleConfigController(RaffleConfigService raffleConfigService) {
        this.raffleConfigService = raffleConfigService;
    }

    @Operation(summary = "Get raffle config for admin")
    @GetMapping
    public ResponseEntity<RaffleConfigResponse> getConfig() {
        return ResponseEntity.ok(raffleConfigService.getConfig());
    }

    @Operation(summary = "Update current unit price")
    @PutMapping("/unit-price")
    public ResponseEntity<RaffleConfigResponse> updateUnitPrice(
            @Valid @RequestBody RaffleConfigUnitPriceRequest request) {
        return ResponseEntity.ok(raffleConfigService.updateUnitPrice(request.unitPrice()));
    }

    @Operation(summary = "Update scheduled draw date and time")
    @PutMapping("/scheduled-at")
    public ResponseEntity<RaffleConfigResponse> updateScheduledDrawAt(
            @RequestBody RaffleConfigScheduledDrawAtRequest request) {
        return ResponseEntity.ok(raffleConfigService.updateScheduledDrawAt(request.scheduledDrawAt()));
    }
}
