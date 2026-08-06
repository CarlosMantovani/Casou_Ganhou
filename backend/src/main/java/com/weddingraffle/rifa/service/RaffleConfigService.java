package com.weddingraffle.rifa.service;

import com.weddingraffle.rifa.dto.RaffleConfigResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface RaffleConfigService {

    RaffleConfigResponse getConfig();

    BigDecimal currentUnitPrice();

    RaffleConfigResponse updateUnitPrice(BigDecimal unitPrice);

    RaffleConfigResponse updateScheduledDrawAt(OffsetDateTime scheduledDrawAt);
}
