package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.dto.CashTransactionCreateRequest;
import com.weddingraffle.rifa.entity.LuckyNumber;
import com.weddingraffle.rifa.entity.ParticipantFlag;
import com.weddingraffle.rifa.entity.PaymentMethod;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.repository.LuckyNumberRepository;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.LuckyNumberService;
import com.weddingraffle.rifa.service.ParticipantFlagService;
import com.weddingraffle.rifa.service.PaymentApprovedEvent;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminTransactionServiceImplTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LuckyNumberRepository luckyNumberRepository;

    @Mock
    private LuckyNumberService luckyNumberService;

    @Mock
    private ParticipantFlagService participantFlagService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void listsTransactionsWithLuckyNumbers() {
        AdminTransactionServiceImpl service = service();
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
    void filtersTransactionsByNameOrEmailWhenProvided() {
        AdminTransactionServiceImpl service = service();
        Transaction transaction =
                new Transaction("guest@example.com", 1, new BigDecimal("10.00"), PaymentStatus.PENDING, "external");
        PageRequest pageable = PageRequest.of(0, 20);
        when(transactionRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        "guest", "guest", pageable))
                .thenReturn(new PageImpl<>(List.of(transaction), pageable, 1));
        when(luckyNumberRepository.findByTransactionInOrderByNumberAsc(List.of(transaction)))
                .thenReturn(List.of());

        var response = service.list("guest", pageable);

        assertThat(response.getContent().getFirst().email()).isEqualTo("guest@example.com");
        assertThat(response.getContent().getFirst().luckyNumbers()).isEmpty();
    }

    @Test
    void createsApprovedCashTransactionWithLuckyNumbers() {
        AdminTransactionServiceImpl service = service();
        when(participantFlagService.resolveForPhone("11999999999"))
                .thenReturn(new ParticipantFlag("BRAZIL", "Brasil", "BR"));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(luckyNumberService.generateFor(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            return List.of(new LuckyNumber("00001", transaction.getEmail(), transaction));
        });

        var response = service.createCashTransaction(
                new CashTransactionCreateRequest("Guest User", "(11) 99999-9999", "GUEST@example.com", 2));

        assertThat(response.name()).isEqualTo("Guest User");
        assertThat(response.phone()).isEqualTo("11999999999");
        assertThat(response.email()).isEqualTo("guest@example.com");
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(response.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.totalAmount()).isEqualByComparingTo("20.00");
        assertThat(response.luckyNumbers()).containsExactly("00001");
        var transactionCaptor = org.mockito.ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getParticipantFlagCode()).isEqualTo("BRAZIL");
        verify(luckyNumberService).generateFor(any(Transaction.class));
        verify(applicationEventPublisher).publishEvent(any(PaymentApprovedEvent.class));
    }

    private AdminTransactionServiceImpl service() {
        return new AdminTransactionServiceImpl(
                appProperties(),
                transactionRepository,
                luckyNumberRepository,
                luckyNumberService,
                participantFlagService,
                applicationEventPublisher);
    }

    private static AppProperties appProperties() {
        return new AppProperties(
                "http://localhost:5173",
                new AppProperties.Jwt("01234567890123456789012345678901", 3600, "raffle-api-test"),
                new AppProperties.Raffle(new BigDecimal("10.00"), "00000", "99999"),
                new AppProperties.MercadoPago(
                        "token",
                        "http://localhost:8080/payments/webhook",
                        "",
                        "http://localhost:5173/payment-return/success",
                        "http://localhost:5173/payment-return/failure",
                        "http://localhost:5173/payment-return/pending",
                        new AppProperties.Retry(3, 500, 2)),
                new AppProperties.Mail("no-reply@example.com"));
    }
}
