package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.weddingraffle.rifa.entity.RaffleConfig;
import com.weddingraffle.rifa.repository.RaffleConfigRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RaffleConfigServiceImplTests {

    @Mock
    private RaffleConfigRepository raffleConfigRepository;

    @Test
    void updatesUnitPriceWithoutChangingScheduledDrawAt() {
        RaffleConfig config = new RaffleConfig(new BigDecimal("10.00"));
        config.updateScheduledDrawAt(OffsetDateTime.parse("2026-09-05T21:00:00-03:00"));
        when(raffleConfigRepository.findById(RaffleConfig.SINGLETON_ID)).thenReturn(Optional.of(config));
        when(raffleConfigRepository.save(config)).thenReturn(config);
        RaffleConfigServiceImpl service = new RaffleConfigServiceImpl(raffleConfigRepository);

        var response = service.updateUnitPrice(new BigDecimal("15.00"));

        assertThat(response.unitPrice()).isEqualByComparingTo("15.00");
        assertThat(response.scheduledDrawAt()).isEqualTo(OffsetDateTime.parse("2026-09-05T21:00:00-03:00"));
    }
}
