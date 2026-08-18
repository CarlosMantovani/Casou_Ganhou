package com.weddingraffle.rifa.dto;

import java.math.BigDecimal;
import java.util.List;

public record TransactionStatusResponse(
        String externalReference,
        boolean emailProvided,
        PaymentStatusResponse status,
        Integer quantity,
        BigDecimal totalAmount,
        String participantFlagName,
        String participantFlagEmoji,
        List<String> luckyNumbers) {}
