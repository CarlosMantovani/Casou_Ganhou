package com.weddingraffle.rifa.dto;

import com.weddingraffle.rifa.entity.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;

public record AdminTransactionResponse(
        String externalReference,
        String email,
        Integer quantity,
        BigDecimal totalAmount,
        PaymentStatus status,
        List<String> luckyNumbers) {}
