package com.weddingraffle.rifa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransactionQuoteRequest(@NotBlank @Email String email, @NotNull @Min(value = 1) Integer quantity) {}
