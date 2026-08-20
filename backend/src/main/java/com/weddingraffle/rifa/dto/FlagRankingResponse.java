package com.weddingraffle.rifa.dto;

import java.math.BigDecimal;

public record FlagRankingResponse(String code, String name, String emoji, int position, BigDecimal progressPercent) {}
