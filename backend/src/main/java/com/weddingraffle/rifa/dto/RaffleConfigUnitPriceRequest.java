package com.weddingraffle.rifa.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RaffleConfigUnitPriceRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal unitPrice) {}
