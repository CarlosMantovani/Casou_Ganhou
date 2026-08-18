package com.weddingraffle.rifa.controller;

import com.weddingraffle.rifa.dto.RaffleConfigResponse;
import com.weddingraffle.rifa.dto.UnitPriceUpdateRequest;
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

    @Operation(summary = "Get raffle configuration for admin")
    @GetMapping
    public ResponseEntity<RaffleConfigResponse> getConfig() {
        return ResponseEntity.ok(raffleConfigService.getConfig());
    }

    @Operation(summary = "Update raffle unit price")
    @PutMapping("/unit-price")
    public ResponseEntity<RaffleConfigResponse> updateUnitPrice(@Valid @RequestBody UnitPriceUpdateRequest request) {
        return ResponseEntity.ok(raffleConfigService.updateUnitPrice(request.unitPrice()));
    }
}
