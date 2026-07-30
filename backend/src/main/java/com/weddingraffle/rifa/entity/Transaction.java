package com.weddingraffle.rifa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity(name = "RaffleTransaction")
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "transaction_status")
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, unique = true)
    private String externalReference;

    private String mpPaymentId;

    private String mpPreferenceId;

    private OffsetDateTime confirmationEmailSentAt;

    private OffsetDateTime confirmationEmailFailedAt;

    @Column(length = 500)
    private String confirmationEmailLastError;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Transaction() {}

    public Transaction(
            String email, Integer quantity, BigDecimal totalAmount, PaymentStatus status, String externalReference) {
        this.email = email;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = status;
        this.externalReference = externalReference;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getMpPaymentId() {
        return mpPaymentId;
    }

    public String getMpPreferenceId() {
        return mpPreferenceId;
    }

    public OffsetDateTime getConfirmationEmailSentAt() {
        return confirmationEmailSentAt;
    }

    public OffsetDateTime getConfirmationEmailFailedAt() {
        return confirmationEmailFailedAt;
    }

    public String getConfirmationEmailLastError() {
        return confirmationEmailLastError;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markPayment(PaymentStatus status, String mpPaymentId) {
        this.status = status;
        this.mpPaymentId = mpPaymentId;
    }

    public void assignPreference(String mpPreferenceId) {
        this.mpPreferenceId = mpPreferenceId;
    }

    public void markConfirmationEmailSent(OffsetDateTime sentAt) {
        this.confirmationEmailSentAt = sentAt;
        this.confirmationEmailFailedAt = null;
        this.confirmationEmailLastError = null;
    }

    public void markConfirmationEmailFailed(OffsetDateTime failedAt, String error) {
        this.confirmationEmailFailedAt = failedAt;
        this.confirmationEmailLastError = error;
    }
}
