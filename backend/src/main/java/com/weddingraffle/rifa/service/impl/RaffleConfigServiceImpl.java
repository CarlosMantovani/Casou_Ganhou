package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.dto.RaffleConfigResponse;
import com.weddingraffle.rifa.entity.RaffleConfig;
import com.weddingraffle.rifa.exception.ResourceNotFoundException;
import com.weddingraffle.rifa.repository.RaffleConfigRepository;
import com.weddingraffle.rifa.service.RaffleConfigService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RaffleConfigServiceImpl implements RaffleConfigService {

    private final RaffleConfigRepository raffleConfigRepository;

    public RaffleConfigServiceImpl(RaffleConfigRepository raffleConfigRepository) {
        this.raffleConfigRepository = raffleConfigRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RaffleConfigResponse getConfig() {
        return toResponse(getSingletonConfig());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal currentUnitPrice() {
        return getSingletonConfig().getUnitPrice();
    }

    @Override
    @Transactional
    public RaffleConfigResponse updateUnitPrice(BigDecimal unitPrice) {
        RaffleConfig config = getSingletonConfig();
        config.updateUnitPrice(unitPrice);
        return toResponse(raffleConfigRepository.save(config));
    }

    @Override
    @Transactional
    public RaffleConfigResponse updateScheduledDrawAt(OffsetDateTime scheduledDrawAt) {
        RaffleConfig config = getSingletonConfig();
        config.updateScheduledDrawAt(scheduledDrawAt);
        return toResponse(raffleConfigRepository.save(config));
    }

    private RaffleConfig getSingletonConfig() {
        return raffleConfigRepository
                .findById(RaffleConfig.SINGLETON_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Raffle config not found."));
    }

    private static RaffleConfigResponse toResponse(RaffleConfig config) {
        return new RaffleConfigResponse(config.getUnitPrice(), config.getScheduledDrawAt());
    }
}
