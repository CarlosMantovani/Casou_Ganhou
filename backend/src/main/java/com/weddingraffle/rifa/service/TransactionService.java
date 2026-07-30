package com.weddingraffle.rifa.service;

import com.weddingraffle.rifa.dto.TransactionQuoteRequest;
import com.weddingraffle.rifa.dto.TransactionQuoteResponse;

public interface TransactionService {

    TransactionQuoteResponse quote(TransactionQuoteRequest request);
}
