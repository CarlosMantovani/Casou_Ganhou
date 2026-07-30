package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.weddingraffle.rifa.entity.LuckyNumber;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.repository.LuckyNumberRepository;
import com.weddingraffle.rifa.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminTransactionServiceImplTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LuckyNumberRepository luckyNumberRepository;

    @Test
    void listsTransactionsWithLuckyNumbers() {
        AdminTransactionServiceImpl service =
                new AdminTransactionServiceImpl(transactionRepository, luckyNumberRepository);
        Transaction transaction =
                new Transaction("guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.APPROVED, "external");
        LuckyNumber first = new LuckyNumber("00001", "guest@example.com", transaction);
        LuckyNumber second = new LuckyNumber("00002", "guest@example.com", transaction);
        PageRequest pageable = PageRequest.of(0, 20);
        when(transactionRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(transaction), pageable, 1));
        when(luckyNumberRepository.findByTransactionInOrderByNumberAsc(List.of(transaction)))
                .thenReturn(List.of(first, second));

        var response = service.list(null, pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().getFirst().externalReference()).isEqualTo("external");
        assertThat(response.getContent().getFirst().luckyNumbers()).containsExactly("00001", "00002");
    }

    @Test
    void filtersTransactionsByEmailWhenProvided() {
        AdminTransactionServiceImpl service =
                new AdminTransactionServiceImpl(transactionRepository, luckyNumberRepository);
        Transaction transaction =
                new Transaction("guest@example.com", 1, new BigDecimal("10.00"), PaymentStatus.PENDING, "external");
        PageRequest pageable = PageRequest.of(0, 20);
        when(transactionRepository.findByEmailContainingIgnoreCase("guest", pageable))
                .thenReturn(new PageImpl<>(List.of(transaction), pageable, 1));
        when(luckyNumberRepository.findByTransactionInOrderByNumberAsc(List.of(transaction)))
                .thenReturn(List.of());

        var response = service.list("guest", pageable);

        assertThat(response.getContent().getFirst().email()).isEqualTo("guest@example.com");
        assertThat(response.getContent().getFirst().luckyNumbers()).isEmpty();
    }
}
