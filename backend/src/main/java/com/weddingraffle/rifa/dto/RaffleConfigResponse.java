package com.weddingraffle.rifa.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RaffleConfigResponse(
        BigDecimal unitPrice,
        OffsetDateTime scheduledDrawAt,
        WeddingProfileResponse weddingProfile,
        OffsetDateTime updatedAt) {}
