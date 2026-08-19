package com.weddingraffle.rifa.repository;

import java.math.BigDecimal;

public interface AdminTransactionSummaryProjection {

    long getTotalTransactions();

    long getApprovedLuckyNumbers();

    BigDecimal getApprovedRevenue();
}
