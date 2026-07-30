package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.dto.TransactionCreateRequest;
import com.weddingraffle.rifa.dto.TransactionCreateResponse;
import com.weddingraffle.rifa.dto.TransactionQuoteRequest;
import com.weddingraffle.rifa.dto.TransactionQuoteResponse;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.integration.CheckoutPreferenceRequest;
import com.weddingraffle.rifa.integration.CheckoutPreferenceResponse;
import com.weddingraffle.rifa.integration.PaymentProviderClient;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.TransactionService;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final AppProperties appProperties;
    private final TransactionRepository transactionRepository;
    private final PaymentProviderClient paymentProviderClient;

    public TransactionServiceImpl(
            AppProperties appProperties,
            TransactionRepository transactionRepository,
            PaymentProviderClient paymentProviderClient) {
        this.appProperties = appProperties;
        this.transactionRepository = transactionRepository;
        this.paymentProviderClient = paymentProviderClient;
    }

    @Override
    public TransactionQuoteResponse quote(TransactionQuoteRequest request) {
        BigDecimal unitPrice = appProperties.raffle().unitPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));
        return new TransactionQuoteResponse(request.email(), request.quantity(), unitPrice, totalAmount);
    }

    @Override
    @Transactional
    public TransactionCreateResponse create(TransactionCreateRequest request) {
        String externalReference = UUID.randomUUID().toString();
        BigDecimal unitPrice = appProperties.raffle().unitPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));

        CheckoutPreferenceResponse preference = paymentProviderClient.createPreference(
                new CheckoutPreferenceRequest(request.email(), request.quantity(), unitPrice, externalReference));

        Transaction transaction = new Transaction(
                request.email(), request.quantity(), totalAmount, PaymentStatus.PENDING, externalReference);
        transaction.assignPreference(preference.preferenceId());
        transactionRepository.save(transaction);

        LOGGER.info("Created pending transaction with externalReference={}", externalReference);

        return new TransactionCreateResponse(externalReference, preference.preferenceId(), preference.checkoutUrl());
    }
}
