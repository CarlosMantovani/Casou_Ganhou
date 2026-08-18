package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.dto.RaffleDrawResponse;
import com.weddingraffle.rifa.entity.LuckyNumber;
import com.weddingraffle.rifa.entity.PaymentStatus;
import com.weddingraffle.rifa.entity.RaffleDraw;
import com.weddingraffle.rifa.exception.InvalidRaffleStateException;
import com.weddingraffle.rifa.exception.ResourceNotFoundException;
import com.weddingraffle.rifa.repository.LuckyNumberRepository;
import com.weddingraffle.rifa.repository.RaffleDrawRepository;
import com.weddingraffle.rifa.service.RaffleService;
import com.weddingraffle.rifa.service.RaffleWinnerSelector;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RaffleServiceImpl implements RaffleService {

    private final RaffleDrawRepository raffleDrawRepository;
    private final LuckyNumberRepository luckyNumberRepository;
    private final RaffleWinnerSelector raffleWinnerSelector;

    public RaffleServiceImpl(
            RaffleDrawRepository raffleDrawRepository,
            LuckyNumberRepository luckyNumberRepository,
            RaffleWinnerSelector raffleWinnerSelector) {
        this.raffleDrawRepository = raffleDrawRepository;
        this.luckyNumberRepository = luckyNumberRepository;
        this.raffleWinnerSelector = raffleWinnerSelector;
    }

    @Override
    @Transactional
    public RaffleDrawResponse draw() {
        return drawNewWinner();
    }

    @Override
    @Transactional(readOnly = true)
    public RaffleDrawResponse getResult() {
        return raffleDrawRepository
                .findFirstByOrderByIdDesc()
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Raffle result not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listEligibleNumbers() {
        return getEligibleLuckyNumbers().stream().map(LuckyNumber::getNumber).toList();
    }

    private RaffleDrawResponse drawNewWinner() {
        List<LuckyNumber> eligibleLuckyNumbers = getEligibleLuckyNumbers();
        LuckyNumber winner = eligibleLuckyNumbers.get(raffleWinnerSelector.selectIndex(eligibleLuckyNumbers.size()));
        RaffleDraw raffleDraw = raffleDrawRepository.save(
                new RaffleDraw(winner.getNumber(), winner.getTransaction().getName(), winner.getEmail()));
        return toResponse(raffleDraw);
    }

    private List<LuckyNumber> getEligibleLuckyNumbers() {
        List<LuckyNumber> eligibleLuckyNumbers = luckyNumberRepository.findEligibleForDraw(PaymentStatus.APPROVED);
        if (eligibleLuckyNumbers.isEmpty()) {
            throw new InvalidRaffleStateException("No eligible lucky numbers for draw.");
        }
        return eligibleLuckyNumbers;
    }

    private RaffleDrawResponse toResponse(RaffleDraw raffleDraw) {
        return new RaffleDrawResponse(
                raffleDraw.getWinningNumber(), raffleDraw.getWinnerName(), raffleDraw.getDrawnAt());
    }
}
