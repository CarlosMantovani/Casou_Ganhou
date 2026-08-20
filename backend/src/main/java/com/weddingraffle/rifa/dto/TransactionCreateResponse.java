package com.weddingraffle.rifa.dto;

public record TransactionCreateResponse(
        String externalReference, String recoveryCode, String preferenceId, String checkoutUrl) {}
