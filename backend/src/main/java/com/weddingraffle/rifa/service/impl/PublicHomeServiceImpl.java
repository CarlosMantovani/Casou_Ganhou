package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.dto.FlagRankingResponse;
import com.weddingraffle.rifa.dto.HomeSummaryResponse;
import com.weddingraffle.rifa.entity.RaffleConfig;
import com.weddingraffle.rifa.exception.ResourceNotFoundException;
import com.weddingraffle.rifa.repository.RaffleConfigRepository;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.PublicHomeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicHomeServiceImpl implements PublicHomeService {

    private static final int FLAG_RANKING_SIZE = 5;

    private final RaffleConfigRepository raffleConfigRepository;
    private final TransactionRepository transactionRepository;

    public PublicHomeServiceImpl(
            RaffleConfigRepository raffleConfigRepository, TransactionRepository transactionRepository) {
        this.raffleConfigRepository = raffleConfigRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public HomeSummaryResponse getSummary() {
        var flagRanking = transactionRepository.findApprovedFlagRanking(PageRequest.of(0, FLAG_RANKING_SIZE)).stream()
                .map(flag -> new FlagRankingResponse(
                        flag.getCode(), flag.getName(), flag.getEmoji(), flag.getTotalNumbers()))
                .toList();
        var config = raffleConfigRepository
                .findById(RaffleConfig.SINGLETON_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Raffle config not found."));
        return new HomeSummaryResponse(
                config.getScheduledDrawAt(), RaffleConfigServiceImpl.toWeddingProfile(config), flagRanking);
    }
}
