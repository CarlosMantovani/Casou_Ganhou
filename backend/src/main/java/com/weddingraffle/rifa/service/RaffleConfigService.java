package com.weddingraffle.rifa.service;

import com.weddingraffle.rifa.dto.RaffleConfigResponse;
import java.math.BigDecimal;

public interface RaffleConfigService {

    BigDecimal getCurrentUnitPrice();

    RaffleConfigResponse getConfig();

    RaffleConfigResponse updateUnitPrice(BigDecimal unitPrice);
}
