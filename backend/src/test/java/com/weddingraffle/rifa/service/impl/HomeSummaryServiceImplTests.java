package com.weddingraffle.rifa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.weddingraffle.rifa.dto.RaffleConfigResponse;
import com.weddingraffle.rifa.repository.TopBuyerProjection;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.RaffleConfigService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeSummaryServiceImplTests {

    @Mock
    private RaffleConfigService raffleConfigService;

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void returnsScheduledDrawAtAndDeterministicAnonymousTopBuyers() {
        OffsetDateTime scheduledDrawAt = OffsetDateTime.parse("2026-09-05T21:00:00-03:00");
        when(raffleConfigService.getConfig())
                .thenReturn(new RaffleConfigResponse(new BigDecimal("10.00"), scheduledDrawAt));
        when(transactionRepository.findTopApprovedBuyers()).thenReturn(List.of(topBuyer("11999999999", 4L)));
        HomeSummaryServiceImpl service = new HomeSummaryServiceImpl(raffleConfigService, transactionRepository);

        var first = service.getSummary();
        var second = service.getSummary();

        assertThat(first.scheduledDrawAt()).isEqualTo(scheduledDrawAt);
        assertThat(first.topBuyers()).hasSize(1);
        assertThat(first.topBuyers().getFirst().quantity()).isEqualTo(4L);
        assertThat(first.topBuyers().getFirst().avatarEmoji()).isNotBlank();
        assertThat(first.topBuyers().getFirst().avatarColor()).startsWith("#");
        assertThat(first.topBuyers().getFirst()).isEqualTo(second.topBuyers().getFirst());
    }

    private static TopBuyerProjection topBuyer(String phone, Long quantity) {
        return new TopBuyerProjection() {
            @Override
            public String getPhone() {
                return phone;
            }

            @Override
            public Long getQuantity() {
                return quantity;
            }
        };
    }
}
