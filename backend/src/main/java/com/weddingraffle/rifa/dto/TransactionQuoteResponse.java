package com.weddingraffle.rifa.dto;

import java.math.BigDecimal;

public record TransactionQuoteResponse(
        String name, String phone, String email, Integer quantity, BigDecimal unitPrice, BigDecimal totalAmount) {}
