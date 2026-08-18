package com.weddingraffle.rifa.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record ScheduledDrawAtUpdateRequest(@NotNull OffsetDateTime scheduledDrawAt) {}
