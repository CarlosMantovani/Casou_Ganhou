package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddingraffle.rifa.entity.LuckyNumber;
import com.weddingraffle.rifa.entity.ParticipantFlag;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.Transaction;
import com.weddingraffle.rifa.exception.InvalidRaffleStateException;
import com.weddingraffle.rifa.exception.ResourceNotFoundException;
import com.weddingraffle.rifa.repository.LuckyNumberRepository;
import com.weddingraffle.rifa.repository.RaffleDrawRepository;
import com.weddingraffle.rifa.service.RaffleWinnerSelector;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RaffleServiceImplTests {

    @Mock
    private RaffleDrawRepository raffleDrawRepository;

    @Mock
    private LuckyNumberRepository luckyNumberRepository;

    @Mock
    private RaffleWinnerSelector raffleWinnerSelector;

    @Test
    void drawsOneWinnerFromApprovedLuckyNumbers() {
        RaffleServiceImpl raffleService =
                new RaffleServiceImpl(raffleDrawRepository, luckyNumberRepository, raffleWinnerSelector);
        Transaction approvedTransaction = new Transaction(
                "Guest User",
                "11999999999",
                "guest@example.com",
                2,
                new BigDecimal("20.00"),
                PaymentStatus.APPROVED,
                com.weddingraffle.rifa.entity.PaymentMethod.MERCADO_PAGO,
                "external");
        LuckyNumber first = new LuckyNumber("00001", "guest@example.com", approvedTransaction);
        LuckyNumber second = new LuckyNumber("00002", "guest@example.com", approvedTransaction);
        approvedTransaction.assignParticipantFlag(new ParticipantFlag("BRAZIL", "Brasil", "🇧🇷"));
        when(luckyNumberRepository.findEligibleForDraw(PaymentStatus.APPROVED)).thenReturn(List.of(first, second));
        when(raffleWinnerSelector.selectIndex(2)).thenReturn(1);
        when(raffleDrawRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = raffleService.draw();

        assertThat(response.winningNumber()).isEqualTo("00002");
        assertThat(response.winnerName()).isEqualTo("Guest User");
        assertThat(response.participantFlagName()).isEqualTo("Brasil");
        assertThat(response.participantFlagEmoji()).isEqualTo("🇧🇷");
        assertThat(response.drawnAt()).isNotNull();
    }

    @Test
    void drawCanCreateANewResultWhenAResultAlreadyExists() {
        RaffleServiceImpl raffleService =
                new RaffleServiceImpl(raffleDrawRepository, luckyNumberRepository, raffleWinnerSelector);
        Transaction approvedTransaction = new Transaction(
                "Guest User",
                "11999999999",
                "guest@example.com",
                1,
                new BigDecimal("10.00"),
                PaymentStatus.APPROVED,
                com.weddingraffle.rifa.entity.PaymentMethod.MERCADO_PAGO,
                "external");
        LuckyNumber luckyNumber = new LuckyNumber("00003", "guest@example.com", approvedTransaction);
        approvedTransaction.assignParticipantFlag(new ParticipantFlag("CANADA", "Canada", "🇨🇦"));
        when(luckyNumberRepository.findEligibleForDraw(PaymentStatus.APPROVED)).thenReturn(List.of(luckyNumber));
        when(raffleWinnerSelector.selectIndex(1)).thenReturn(0);
        when(raffleDrawRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = raffleService.draw();

        assertThat(response.winningNumber()).isEqualTo("00003");
        assertThat(response.participantFlagName()).isEqualTo("Canada");
        verify(raffleDrawRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void drawFailsWhenThereAreNoEligibleLuckyNumbers() {
        RaffleServiceImpl raffleService =
                new RaffleServiceImpl(raffleDrawRepository, luckyNumberRepository, raffleWinnerSelector);
        when(luckyNumberRepository.findEligibleForDraw(PaymentStatus.APPROVED)).thenReturn(List.of());

        assertThatThrownBy(raffleService::draw).isInstanceOf(InvalidRaffleStateException.class);
    }

    @Test
    void getResultFailsWhenResultDoesNotExist() {
        RaffleServiceImpl raffleService =
                new RaffleServiceImpl(raffleDrawRepository, luckyNumberRepository, raffleWinnerSelector);
        when(raffleDrawRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());

        assertThatThrownBy(raffleService::getResult).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listEligibleNumbersReturnsApprovedLuckyNumbersOnly() {
        RaffleServiceImpl raffleService =
                new RaffleServiceImpl(raffleDrawRepository, luckyNumberRepository, raffleWinnerSelector);
        Transaction approvedTransaction = new Transaction(
                "Guest User",
                "11999999999",
                "guest@example.com",
                2,
                new BigDecimal("20.00"),
                PaymentStatus.APPROVED,
                com.weddingraffle.rifa.entity.PaymentMethod.MERCADO_PAGO,
                "external");
        LuckyNumber first = new LuckyNumber("00001", "guest@example.com", approvedTransaction);
        LuckyNumber second = new LuckyNumber("00002", "guest@example.com", approvedTransaction);
        approvedTransaction.assignParticipantFlag(new ParticipantFlag("JAPAN", "Japao", "🇯🇵"));
        when(luckyNumberRepository.findEligibleForDraw(PaymentStatus.APPROVED)).thenReturn(List.of(first, second));

        assertThat(raffleService.listEligibleNumbers())
                .extracting("luckyNumber")
                .containsExactly("00001", "00002");
        assertThat(raffleService.listEligibleNumbers())
                .extracting("participantFlagName")
                .containsExactly("Japao", "Japao");
    }
}
