package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RandomRecoveryCodeServiceTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void reusesExistingRecoveryCodeForPhone() {
        Transaction transaction = new Transaction(
                "Guest User",
                "11999999999",
                null,
                1,
                new BigDecimal("10.00"),
                PaymentStatus.PENDING,
                com.weddingraffle.rifa.entity.PaymentMethod.MERCADO_PAGO,
                "external-reference");
        transaction.assignRecoveryCode("4821");
        when(transactionRepository.findFirstByPhoneOrderByCreatedAtAsc("11999999999"))
                .thenReturn(Optional.of(transaction));

        String recoveryCode = new RandomRecoveryCodeService(transactionRepository).resolveForPhone("11999999999");

        assertThat(recoveryCode).isEqualTo("4821");
        verify(transactionRepository, never()).existsByRecoveryCode("4821");
    }

    @Test
    void generatesUnusedRecoveryCodeForNewPhone() {
        when(transactionRepository.findFirstByPhoneOrderByCreatedAtAsc("11999999999"))
                .thenReturn(Optional.empty());
        when(transactionRepository.existsByRecoveryCode(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(false);

        String recoveryCode = new RandomRecoveryCodeService(transactionRepository).resolveForPhone("11999999999");

        assertThat(recoveryCode).matches("\\d{4}");
    }
}
