package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.dto.AdminTransactionResponse;
import com.weddingraffle.rifa.dto.CashTransactionCreateRequest;
import com.weddingraffle.rifa.dto.CashTransactionCreateResponse;
import com.weddingraffle.rifa.entity.LuckyNumber;
import com.weddingraffle.rifa.entity.PaymentMethod;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.repository.LuckyNumberRepository;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.AdminTransactionService;
import com.weddingraffle.rifa.service.LuckyNumberService;
import com.weddingraffle.rifa.service.PaymentApprovedEvent;
import com.weddingraffle.rifa.util.ParticipantNormalizer;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminTransactionServiceImpl implements AdminTransactionService {

    private final AppProperties appProperties;
    private final TransactionRepository transactionRepository;
    private final LuckyNumberRepository luckyNumberRepository;
    private final LuckyNumberService luckyNumberService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AdminTransactionServiceImpl(
            AppProperties appProperties,
            TransactionRepository transactionRepository,
            LuckyNumberRepository luckyNumberRepository,
            LuckyNumberService luckyNumberService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.appProperties = appProperties;
        this.transactionRepository = transactionRepository;
        this.luckyNumberRepository = luckyNumberRepository;
        this.luckyNumberService = luckyNumberService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminTransactionResponse> list(String query, Pageable pageable) {
        Page<Transaction> transactions = StringUtils.hasText(query)
                ? transactionRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        query, query, pageable)
                : transactionRepository.findAll(pageable);
        Map<Transaction, List<String>> numbersByTransaction = numbersByTransaction(transactions.getContent());
        return transactions.map(transaction -> toResponse(transaction, numbersByTransaction));
    }

    @Override
    @Transactional
    public CashTransactionCreateResponse createCashTransaction(CashTransactionCreateRequest request) {
        String name = ParticipantNormalizer.normalizeName(request.name());
        String phone = ParticipantNormalizer.normalizePhone(request.phone());
        String email = ParticipantNormalizer.normalizeEmail(request.email());
        BigDecimal unitPrice = appProperties.raffle().unitPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));

        Transaction transaction = transactionRepository.save(new Transaction(
                name,
                phone,
                email,
                request.quantity(),
                unitPrice,
                totalAmount,
                PaymentStatus.APPROVED,
                PaymentMethod.CASH,
                UUID.randomUUID().toString()));
        List<String> luckyNumbers = luckyNumberService.generateFor(transaction).stream()
                .map(LuckyNumber::getNumber)
                .sorted()
                .toList();

        applicationEventPublisher.publishEvent(new PaymentApprovedEvent(transaction.getExternalReference()));

        return new CashTransactionCreateResponse(
                transaction.getExternalReference(),
                transaction.getName(),
                transaction.getPhone(),
                transaction.getEmail(),
                transaction.getPaymentMethod(),
                transaction.getStatus(),
                transaction.getQuantity(),
                transaction.getTotalAmount(),
                luckyNumbers);
    }

    private Map<Transaction, List<String>> numbersByTransaction(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return Collections.emptyMap();
        }
        return luckyNumberRepository.findByTransactionInOrderByNumberAsc(transactions).stream()
                .collect(Collectors.groupingBy(
                        LuckyNumber::getTransaction, Collectors.mapping(LuckyNumber::getNumber, Collectors.toList())));
    }

    private static AdminTransactionResponse toResponse(
            Transaction transaction, Map<Transaction, List<String>> numbersByTransaction) {
        return new AdminTransactionResponse(
                transaction.getExternalReference(),
                transaction.getCreatedAt(),
                transaction.getName(),
                transaction.getPhone(),
                transaction.getEmail(),
                transaction.getPaymentMethod(),
                transaction.getQuantity(),
                transaction.getTotalAmount(),
                transaction.getStatus(),
                numbersByTransaction.getOrDefault(transaction, List.of()));
    }
}
