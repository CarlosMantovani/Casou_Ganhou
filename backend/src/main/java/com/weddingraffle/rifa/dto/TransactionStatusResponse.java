package com.weddingraffle.rifa.dto;

import com.weddingraffle.rifa.entity.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;

public record TransactionStatusResponse(
        String externalReference,
        boolean emailProvided,
        PaymentStatus status,
        Integer quantity,
        BigDecimal totalAmount,
        String participantFlagName,
        String participantFlagEmoji,
        List<String> luckyNumbers) {}
