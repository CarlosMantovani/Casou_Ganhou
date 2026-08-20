package com.weddingraffle.rifa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TransactionRecoveryRequest(
        @NotBlank String phone,
        @NotBlank @Pattern(regexp = "\\d{4}", message = "Recovery code must have 4 digits.") String recoveryCode) {}
