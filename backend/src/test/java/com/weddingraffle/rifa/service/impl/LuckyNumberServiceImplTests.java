package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddingraffle.rifa.config.AppProperties;
import com.weddingraffle.rifa.entity.LuckyNumber;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.repository.LuckyNumberRepository;
import com.weddingraffle.rifa.service.LuckyNumberCandidateGenerator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LuckyNumberServiceImplTests {

    @Mock
    private LuckyNumberRepository luckyNumberRepository;

    @Mock
    private LuckyNumberCandidateGenerator candidateGenerator;

    @Test
    void generatesConfiguredQuantityInsideConfiguredRange() {
        LuckyNumberServiceImpl luckyNumberService =
                new LuckyNumberServiceImpl(appProperties(), luckyNumberRepository, candidateGenerator, clock());
        Transaction transaction =
                new Transaction("guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.PENDING, "external");
        when(candidateGenerator.nextInt(0, 99999)).thenReturn(1, 2);
        when(luckyNumberRepository.existsByNumber(any())).thenReturn(false);
        when(luckyNumberRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LuckyNumber> luckyNumbers = luckyNumberService.generateFor(transaction);

        assertThat(luckyNumbers).extracting(LuckyNumber::getNumber).containsExactly("00001", "00002");
        assertThat(luckyNumbers).extracting(LuckyNumber::getAllocationIndex).containsExactly(1, 2);
        assertThat(transaction.getLuckyNumbersGeneratedAt()).isEqualTo(OffsetDateTime.parse("2026-08-22T12:00:00Z"));
        assertThat(luckyNumbers).allSatisfy(luckyNumber -> {
            assertThat(luckyNumber.getEmail()).isEqualTo("guest@example.com");
            assertThat(luckyNumber.getTransaction()).isSameAs(transaction);
        });
    }

    @Test
    void retriesWhenGeneratedNumberAlreadyExists() {
        LuckyNumberServiceImpl luckyNumberService =
                new LuckyNumberServiceImpl(appProperties(), luckyNumberRepository, candidateGenerator, clock());
        Transaction transaction =
                new Transaction("guest@example.com", 1, new BigDecimal("10.00"), PaymentStatus.PENDING, "external");
        when(candidateGenerator.nextInt(0, 99999)).thenReturn(1, 2);
        when(luckyNumberRepository.existsByNumber("00001")).thenReturn(true);
        when(luckyNumberRepository.existsByNumber("00002")).thenReturn(false);
        when(luckyNumberRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LuckyNumber> luckyNumbers = luckyNumberService.generateFor(transaction);

        assertThat(luckyNumbers).extracting(LuckyNumber::getNumber).containsExactly("00002");
    }

    @Test
    void doesNotGenerateAgainWhenTransactionAlreadyHasLuckyNumbers() {
        LuckyNumberServiceImpl luckyNumberService =
                new LuckyNumberServiceImpl(appProperties(), luckyNumberRepository, candidateGenerator, clock());
        Transaction transaction =
                new Transaction("guest@example.com", 1, new BigDecimal("10.00"), PaymentStatus.APPROVED, "external");
        LuckyNumber existingLuckyNumber = new LuckyNumber("00001", "guest@example.com", transaction, 1);
        transaction.markLuckyNumberBatchCompleted(OffsetDateTime.parse("2026-08-22T11:00:00Z"));
        when(luckyNumberRepository.findByTransactionOrderByNumberAsc(transaction))
                .thenReturn(List.of(existingLuckyNumber));

        List<LuckyNumber> luckyNumbers = luckyNumberService.generateFor(transaction);

        assertThat(luckyNumbers).containsExactly(existingLuckyNumber);
        verify(luckyNumberRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void findsNumbersByTransactionExternalReference() {
        LuckyNumberServiceImpl luckyNumberService =
                new LuckyNumberServiceImpl(appProperties(), luckyNumberRepository, candidateGenerator, clock());
        when(luckyNumberRepository.findNumbersByTransactionExternalReference("external"))
                .thenReturn(List.of("00001", "00002"));

        List<String> numbers = luckyNumberService.findNumbers("external");

        assertThat(numbers).containsExactly("00001", "00002");
    }

    @Test
    void findsPreviousApprovedNumbersByPhoneExcludingCurrentTransaction() {
        LuckyNumberServiceImpl luckyNumberService =
                new LuckyNumberServiceImpl(appProperties(), luckyNumberRepository, candidateGenerator, clock());
        when(luckyNumberRepository.findNumbersByPhoneAndStatusExcludingExternalReference(
                        "11999999999", PaymentStatus.APPROVED, "external"))
                .thenReturn(List.of("00001", "00002"));

        List<String> numbers = luckyNumberService.findPreviousApprovedNumbers("11999999999", "external");

        assertThat(numbers).containsExactly("00001", "00002");
    }

    @Test
    void avoidsDuplicatePendingNumbersBeforeSaving() {
        LuckyNumberServiceImpl luckyNumberService =
                new LuckyNumberServiceImpl(appProperties(), luckyNumberRepository, candidateGenerator, clock());
        Transaction transaction =
                new Transaction("guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.PENDING, "external");
        when(candidateGenerator.nextInt(0, 99999)).thenReturn(1, 1, 2);
        when(luckyNumberRepository.existsByNumber(any())).thenReturn(false);
        when(luckyNumberRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        luckyNumberService.generateFor(transaction);

        ArgumentCaptor<Iterable<LuckyNumber>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(luckyNumberRepository).saveAllAndFlush(captor.capture());
        assertThat(captor.getValue()).extracting(LuckyNumber::getNumber).containsExactly("00001", "00002");
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
                        new AppProperties.Retry(3, 500, 2)));
    }

    private static Clock clock() {
        return Clock.fixed(OffsetDateTime.parse("2026-08-22T12:00:00Z").toInstant(), ZoneOffset.UTC);
    }
}
