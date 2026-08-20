package com.weddingraffle.rifa.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransactionQuoteRequest(
        @NotBlank String name, @NotBlank String phone, @NotNull @Min(value = 1) Integer quantity) {}
