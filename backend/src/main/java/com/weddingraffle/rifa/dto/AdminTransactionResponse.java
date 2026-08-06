package com.weddingraffle.rifa.dto;

import com.weddingraffle.rifa.entity.PaymentMethod;
import com.weddingraffle.rifa.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminTransactionResponse(
        String externalReference,
        String name,
        String phone,
        String email,
        PaymentMethod paymentMethod,
        Integer quantity,
        BigDecimal totalAmount,
        BigDecimal unitPrice,
        PaymentStatus status,
        OffsetDateTime createdAt,
        List<String> luckyNumbers) {}
