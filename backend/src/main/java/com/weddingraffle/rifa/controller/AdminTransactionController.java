package com.weddingraffle.rifa.controller;

import com.weddingraffle.rifa.dto.AdminTransactionResponse;
import com.weddingraffle.rifa.dto.CashTransactionCreateRequest;
import com.weddingraffle.rifa.dto.CashTransactionCreateResponse;
import com.weddingraffle.rifa.service.AdminTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String email,
            @PageableDefault(size = 20) Pageable pageable) {
        String resolvedQuery = StringUtils.hasText(query) ? query : email;
        return ResponseEntity.ok(adminTransactionService.list(resolvedQuery, pageable));
    }

    @Operation(summary = "Create approved cash transaction for admin")
    @PostMapping("/cash")
    public ResponseEntity<CashTransactionCreateResponse> createCashTransaction(
            @Valid @RequestBody CashTransactionCreateRequest request) {
        return ResponseEntity.ok(adminTransactionService.createCashTransaction(request));
    }

    @Operation(summary = "Delete cash transaction for admin")
    @DeleteMapping("/{externalReference}")
    public ResponseEntity<Void> deleteCashTransaction(@PathVariable String externalReference) {
        adminTransactionService.deleteCashTransaction(externalReference);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
