package com.weddingraffle.rifa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "raffle_draw")
public class RaffleDraw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String winningNumber;

    @Column(nullable = false)
    private String winnerName;

    @Column(length = 320)
    private String winnerEmail;

    @Column(nullable = false)
    private OffsetDateTime drawnAt;

    protected RaffleDraw() {}

    public RaffleDraw(String winningNumber, String winnerEmail) {
        this(winningNumber, winnerEmail, winnerEmail);
    }

    public RaffleDraw(String winningNumber, String winnerName, String winnerEmail) {
        this.winningNumber = winningNumber;
        this.winnerName = winnerName;
        this.winnerEmail = winnerEmail;
        this.drawnAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getWinningNumber() {
        return winningNumber;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public String getWinnerEmail() {
        return winnerEmail;
    }

    public OffsetDateTime getDrawnAt() {
        return drawnAt;
    }
}
