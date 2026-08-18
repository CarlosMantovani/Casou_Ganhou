package com.weddingraffle.rifa.dto;

import com.weddingraffle.rifa.entity.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;

public record CashTransactionCreateResponse(
        String externalReference,
        String name,
        String phone,
        String email,
        PaymentMethod paymentMethod,
        PaymentStatusResponse status,
        Integer quantity,
        BigDecimal totalAmount,
        List<String> luckyNumbers) {}
