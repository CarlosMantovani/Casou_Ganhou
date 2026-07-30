package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddingraffle.rifa.entity.LuckyNumber;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.RaffleDraw;
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
        Transaction approvedTransaction =
                new Transaction("guest@example.com", 2, new BigDecimal("20.00"), PaymentStatus.APPROVED, "external");
        LuckyNumber first = new LuckyNumber("00001", "guest@example.com", approvedTransaction);
        LuckyNumber second = new LuckyNumber("00002", "guest@example.com", approvedTransaction);
        when(raffleDrawRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(luckyNumberRepository.findEligibleForDraw(PaymentStatus.APPROVED)).thenReturn(List.of(first, second));
        when(raffleWinnerSelector.selectIndex(2)).thenReturn(1);
        when(raffleDrawRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = raffleService.draw();

        assertThat(response.winningNumber()).isEqualTo("00002");
        assertThat(response.winnerEmail()).isEqualTo("guest@example.com");
        assertThat(response.drawnAt()).isNotNull();
    }

    @Test
    void drawReturnsExistingResultWithoutDrawingAgain() {
        RaffleServiceImpl raffleService =
                new RaffleServiceImpl(raffleDrawRepository, luckyNumberRepository, raffleWinnerSelector);
        RaffleDraw existingDraw = new RaffleDraw("00001", "guest@example.com");
        when(raffleDrawRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existingDraw));

        var response = raffleService.draw();

        assertThat(response.winningNumber()).isEqualTo("00001");
        verify(luckyNumberRepository, never()).findEligibleForDraw(PaymentStatus.APPROVED);
        verify(raffleDrawRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void drawFailsWhenThereAreNoEligibleLuckyNumbers() {
        RaffleServiceImpl raffleService =
                new RaffleServiceImpl(raffleDrawRepository, luckyNumberRepository, raffleWinnerSelector);
        when(raffleDrawRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(luckyNumberRepository.findEligibleForDraw(PaymentStatus.APPROVED)).thenReturn(List.of());

        assertThatThrownBy(raffleService::draw).isInstanceOf(InvalidRaffleStateException.class);
    }

    @Test
    void getResultFailsWhenResultDoesNotExist() {
        RaffleServiceImpl raffleService =
                new RaffleServiceImpl(raffleDrawRepository, luckyNumberRepository, raffleWinnerSelector);
        when(raffleDrawRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(raffleService::getResult).isInstanceOf(ResourceNotFoundException.class);
    }
}
