package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.dto.TransactionQuoteRequest;
import com.weddingraffle.rifa.dto.TransactionQuoteResponse;
import com.weddingraffle.rifa.service.TransactionService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final AppProperties appProperties;

    public TransactionServiceImpl(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public TransactionQuoteResponse quote(TransactionQuoteRequest request) {
        BigDecimal unitPrice = appProperties.raffle().unitPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));
        return new TransactionQuoteResponse(request.email(), request.quantity(), unitPrice, totalAmount);
    }
}
