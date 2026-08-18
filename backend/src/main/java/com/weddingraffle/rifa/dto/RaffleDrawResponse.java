package com.weddingraffle.rifa.dto;

import java.time.OffsetDateTime;

public record RaffleDrawResponse(
        String winningNumber,
        String winnerName,
        OffsetDateTime drawnAt,
        String participantFlagName,
        String participantFlagEmoji) {

    public RaffleDrawResponse(String winningNumber, String winnerName, OffsetDateTime drawnAt) {
        this(winningNumber, winnerName, drawnAt, null, null);
    }
}
