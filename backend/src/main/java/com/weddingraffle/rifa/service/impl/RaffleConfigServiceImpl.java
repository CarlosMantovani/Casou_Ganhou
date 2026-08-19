package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.dto.RaffleConfigResponse;
import com.weddingraffle.rifa.entity.RaffleConfig;
import com.weddingraffle.rifa.exception.ResourceNotFoundException;
import com.weddingraffle.rifa.repository.RaffleConfigRepository;
import com.weddingraffle.rifa.service.RaffleConfigService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RaffleConfigServiceImpl implements RaffleConfigService {

    private final RaffleConfigRepository raffleConfigRepository;
    private final EntityManager entityManager;

    public RaffleConfigServiceImpl(RaffleConfigRepository raffleConfigRepository, EntityManager entityManager) {
        this.raffleConfigRepository = raffleConfigRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentUnitPrice() {
        return getCurrentConfig().getUnitPrice();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDrawClosed() {
        OffsetDateTime scheduledDrawAt = getCurrentConfig().getScheduledDrawAt();
        return scheduledDrawAt != null && !scheduledDrawAt.isAfter(OffsetDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public RaffleConfigResponse getConfig() {
        return toResponse(getCurrentConfig());
    }

    @Override
    @Transactional
    public RaffleConfigResponse updateUnitPrice(BigDecimal unitPrice) {
        RaffleConfig config = getCurrentConfig();
        config.updateUnitPrice(unitPrice);
        return saveAndRefresh(config);
    }

    @Override
    @Transactional
    public RaffleConfigResponse updateScheduledDrawAt(OffsetDateTime scheduledDrawAt) {
        RaffleConfig config = getCurrentConfig();
        config.updateScheduledDrawAt(scheduledDrawAt);
        return saveAndRefresh(config);
    }

    private RaffleConfigResponse saveAndRefresh(RaffleConfig config) {
        raffleConfigRepository.saveAndFlush(config);
        entityManager.refresh(config);
        return toResponse(config);
    }

    private RaffleConfig getCurrentConfig() {
        return raffleConfigRepository
                .findById(RaffleConfig.SINGLETON_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Raffle config not found."));
    }

    private static RaffleConfigResponse toResponse(RaffleConfig config) {
        return new RaffleConfigResponse(config.getUnitPrice(), config.getScheduledDrawAt(), config.getUpdatedAt());
    }
}
