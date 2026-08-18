package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.dto.FlagRankingResponse;
import com.weddingraffle.rifa.dto.HomeSummaryResponse;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.PublicHomeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicHomeServiceImpl implements PublicHomeService {

    private static final int FLAG_RANKING_SIZE = 5;

    private final TransactionRepository transactionRepository;

    public PublicHomeServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public HomeSummaryResponse getSummary() {
        var flagRanking = transactionRepository.findApprovedFlagRanking(PageRequest.of(0, FLAG_RANKING_SIZE)).stream()
                .map(flag -> new FlagRankingResponse(
                        flag.getCode(), flag.getName(), flag.getEmoji(), flag.getTotalNumbers()))
                .toList();
        return new HomeSummaryResponse(null, flagRanking);
    }
}
