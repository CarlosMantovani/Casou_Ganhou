package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.RecoveryCodeService;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;

@Service
public class RandomRecoveryCodeService implements RecoveryCodeService {

    private static final int CODE_BOUND = 10_000;
    private static final int CODE_WIDTH = 4;

    private final SecureRandom secureRandom = new SecureRandom();
    private final TransactionRepository transactionRepository;

    public RandomRecoveryCodeService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public String resolveForPhone(String phone) {
        return transactionRepository
                .findFirstByPhoneOrderByCreatedAtAsc(phone)
                .map(Transaction::getRecoveryCode)
                .orElseGet(this::generateUniqueCode);
    }

    private String generateUniqueCode() {
        for (int attempts = 0; attempts < CODE_BOUND; attempts++) {
            String candidate = String.format("%0" + CODE_WIDTH + "d", secureRandom.nextInt(CODE_BOUND));
            if (!transactionRepository.existsByRecoveryCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No recovery codes available.");
    }
}
