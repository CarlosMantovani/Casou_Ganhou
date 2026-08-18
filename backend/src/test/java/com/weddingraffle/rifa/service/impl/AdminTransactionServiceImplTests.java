package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddingraffle.rifa.dto.CashTransactionCreateRequest;
import com.weddingraffle.rifa.entity.LuckyNumber;
import com.weddingraffle.rifa.entity.ParticipantFlag;
import com.weddingraffle.rifa.entity.PaymentMethod;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.exception.InvalidTransactionStateException;
import com.weddingraffle.rifa.repository.LuckyNumberRepository;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.LuckyNumberService;
import com.weddingraffle.rifa.service.ParticipantFlagService;
import com.weddingraffle.rifa.service.PaymentApprovedEvent;
import com.weddingraffle.rifa.service.RaffleConfigService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
    private RaffleConfigService raffleConfigService;

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
                .thenReturn(new ParticipantFlag("BRAZIL", "Brasil", "🇧🇷"));
        when(raffleConfigService.getCurrentUnitPrice()).thenReturn(new BigDecimal("10.00"));
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
        assertThat(transactionCaptor.getValue().getParticipantFlagEmoji()).isEqualTo("🇧🇷");
        verify(luckyNumberService).generateFor(any(Transaction.class));
        verify(applicationEventPublisher).publishEvent(any(PaymentApprovedEvent.class));
    }

    @Test
    void deletesCashTransactionWithLuckyNumbers() {
        AdminTransactionServiceImpl service = service();
        Transaction transaction = new Transaction(
                "Guest User",
                "11999999999",
                null,
                1,
                new BigDecimal("10.00"),
                PaymentStatus.APPROVED,
                PaymentMethod.CASH,
                "cash-reference");
        when(transactionRepository.findByExternalReference("cash-reference")).thenReturn(Optional.of(transaction));

        service.deleteCashTransaction("cash-reference");

        verify(luckyNumberRepository).deleteByTransaction(transaction);
        verify(transactionRepository).delete(transaction);
    }

    @Test
    void deleteCashTransactionRejectsMercadoPagoTransaction() {
        AdminTransactionServiceImpl service = service();
        Transaction transaction = new Transaction(
                "Guest User",
                "11999999999",
                "guest@example.com",
                1,
                new BigDecimal("10.00"),
                PaymentStatus.APPROVED,
                PaymentMethod.MERCADO_PAGO,
                "mp-reference");
        when(transactionRepository.findByExternalReference("mp-reference")).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> service.deleteCashTransaction("mp-reference"))
                .isInstanceOf(InvalidTransactionStateException.class);
    }

    private AdminTransactionServiceImpl service() {
        return new AdminTransactionServiceImpl(
                raffleConfigService,
                transactionRepository,
                luckyNumberRepository,
                luckyNumberService,
                participantFlagService,
                applicationEventPublisher);
    }
}
