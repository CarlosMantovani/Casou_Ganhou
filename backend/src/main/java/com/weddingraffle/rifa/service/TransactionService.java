package com.weddingraffle.rifa.service;

import com.weddingraffle.rifa.dto.TransactionCreateRequest;
import com.weddingraffle.rifa.dto.TransactionCreateResponse;
import com.weddingraffle.rifa.dto.TransactionQuoteRequest;
import com.weddingraffle.rifa.dto.TransactionQuoteResponse;

public interface TransactionService {

    TransactionQuoteResponse quote(TransactionQuoteRequest request);

    TransactionCreateResponse create(TransactionCreateRequest request);
}
