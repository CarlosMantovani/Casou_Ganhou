package com.weddingraffle.rifa.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record HomeSummaryResponse(
        OffsetDateTime scheduledDrawAt, WeddingProfileResponse weddingProfile, List<FlagRankingResponse> flagRanking) {}
