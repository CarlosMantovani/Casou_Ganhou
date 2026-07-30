package com.weddingraffle.rifa.controller;

import com.weddingraffle.rifa.dto.AdminTransactionResponse;
import com.weddingraffle.rifa.service.AdminTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class AdminTransactionController {

    private final AdminTransactionService adminTransactionService;

    public AdminTransactionController(AdminTransactionService adminTransactionService) {
        this.adminTransactionService = adminTransactionService;
    }

    @Operation(summary = "List transactions for admin")
    @GetMapping
    public ResponseEntity<Page<AdminTransactionResponse>> list(
            @RequestParam(required = false) String email, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminTransactionService.list(email, pageable));
    }
}
