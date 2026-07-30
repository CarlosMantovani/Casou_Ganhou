package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.dto.AdminTransactionResponse;
import com.weddingraffle.rifa.entity.LuckyNumber;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.repository.LuckyNumberRepository;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.AdminTransactionService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminTransactionServiceImpl implements AdminTransactionService {

    private final TransactionRepository transactionRepository;
    private final LuckyNumberRepository luckyNumberRepository;

    public AdminTransactionServiceImpl(
            TransactionRepository transactionRepository, LuckyNumberRepository luckyNumberRepository) {
        this.transactionRepository = transactionRepository;
        this.luckyNumberRepository = luckyNumberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminTransactionResponse> list(String email, Pageable pageable) {
        Page<Transaction> transactions = StringUtils.hasText(email)
                ? transactionRepository.findByEmailContainingIgnoreCase(email, pageable)
                : transactionRepository.findAll(pageable);
        Map<Transaction, List<String>> numbersByTransaction = numbersByTransaction(transactions.getContent());
        return transactions.map(transaction -> toResponse(transaction, numbersByTransaction));
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
                transaction.getEmail(),
                transaction.getQuantity(),
                transaction.getTotalAmount(),
                transaction.getStatus(),
                numbersByTransaction.getOrDefault(transaction, List.of()));
    }
}
