package com.weddingraffle.rifa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "raffle_config")
public class RaffleConfig {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    private OffsetDateTime scheduledDrawAt;

    @Column(nullable = false, length = 120)
    private String groomName;

    @Column(nullable = false, length = 120)
    private String brideName;

    @Column(nullable = false, length = 7)
    private String colorIvory;

    @Column(nullable = false, length = 7)
    private String colorIvoryDeep;

    @Column(nullable = false, length = 7)
    private String colorInk;

    @Column(nullable = false, length = 7)
    private String colorInkSoft;

    @Column(nullable = false, length = 7)
    private String colorGreen;

    @Column(nullable = false, length = 7)
    private String colorGreenDeep;

    @Column(nullable = false, length = 7)
    private String colorWine;

    @Column(nullable = false, length = 7)
    private String colorGold;

    @Column(nullable = false, length = 7)
    private String colorGoldSoft;

    @Column(nullable = false, length = 7)
    private String colorLine;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected RaffleConfig() {}

    public RaffleConfig(BigDecimal unitPrice) {
        this.id = SINGLETON_ID;
        this.unitPrice = unitPrice;
        this.groomName = "Jose Carlos";
        this.brideName = "Paula";
        this.colorIvory = "#F7F1E6";
        this.colorIvoryDeep = "#F0E8D8";
        this.colorInk = "#2B2419";
        this.colorInkSoft = "#5B5140";
        this.colorGreen = "#24402E";
        this.colorGreenDeep = "#152A1D";
        this.colorWine = "#7A2E33";
        this.colorGold = "#B8935A";
        this.colorGoldSoft = "#DCC79A";
        this.colorLine = "#D9CBAA";
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public OffsetDateTime getScheduledDrawAt() {
        return scheduledDrawAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getGroomName() {
        return groomName;
    }

    public String getBrideName() {
        return brideName;
    }

    public String getColorIvory() {
        return colorIvory;
    }

    public String getColorIvoryDeep() {
        return colorIvoryDeep;
    }

    public String getColorInk() {
        return colorInk;
    }

    public String getColorInkSoft() {
        return colorInkSoft;
    }

    public String getColorGreen() {
        return colorGreen;
    }

    public String getColorGreenDeep() {
        return colorGreenDeep;
    }

    public String getColorWine() {
        return colorWine;
    }

    public String getColorGold() {
        return colorGold;
    }

    public String getColorGoldSoft() {
        return colorGoldSoft;
    }

    public String getColorLine() {
        return colorLine;
    }

    public void updateUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void updateScheduledDrawAt(OffsetDateTime scheduledDrawAt) {
        this.scheduledDrawAt = scheduledDrawAt;
    }

    public void updateWeddingProfile(
            String groomName,
            String brideName,
            String colorIvory,
            String colorIvoryDeep,
            String colorInk,
            String colorInkSoft,
            String colorGreen,
            String colorGreenDeep,
            String colorWine,
            String colorGold,
            String colorGoldSoft,
            String colorLine) {
        this.groomName = groomName;
        this.brideName = brideName;
        this.colorIvory = colorIvory;
        this.colorIvoryDeep = colorIvoryDeep;
        this.colorInk = colorInk;
        this.colorInkSoft = colorInkSoft;
        this.colorGreen = colorGreen;
        this.colorGreenDeep = colorGreenDeep;
        this.colorWine = colorWine;
        this.colorGold = colorGold;
        this.colorGoldSoft = colorGoldSoft;
        this.colorLine = colorLine;
    }
}
