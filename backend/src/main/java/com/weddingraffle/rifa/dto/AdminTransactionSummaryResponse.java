package com.weddingraffle.rifa.dto;

import java.math.BigDecimal;

public record AdminTransactionSummaryResponse(
        long totalTransactions, long approvedLuckyNumbers, BigDecimal approvedRevenue) {}
