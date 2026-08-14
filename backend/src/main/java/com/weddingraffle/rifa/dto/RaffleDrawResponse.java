package com.weddingraffle.rifa.dto;

import java.time.OffsetDateTime;

public record RaffleDrawResponse(String winningNumber, String winnerName, OffsetDateTime drawnAt) {}
