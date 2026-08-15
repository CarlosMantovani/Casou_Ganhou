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
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity(name = "RaffleTransaction")
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 320)
    private String email;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "transaction_status")
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payment_method")
    private PaymentMethod paymentMethod = PaymentMethod.MERCADO_PAGO;

    @Column(nullable = false, unique = true)
    private String externalReference;

    private String mpPaymentId;

    private String mpPreferenceId;

    @Column(length = 2048)
    private String mpCheckoutUrl;

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
        this(
                email,
                "0000000000",
                email,
                quantity,
                inferUnitPrice(totalAmount, quantity),
                totalAmount,
                status,
                PaymentMethod.MERCADO_PAGO,
                externalReference);
    }

    public Transaction(
            String name,
            String phone,
            String email,
            Integer quantity,
            BigDecimal totalAmount,
            PaymentStatus status,
            PaymentMethod paymentMethod,
            String externalReference) {
        this(
                name,
                phone,
                email,
                quantity,
                inferUnitPrice(totalAmount, quantity),
                totalAmount,
                status,
                paymentMethod,
                externalReference);
    }

    public Transaction(
            String name,
            String phone,
            String email,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            PaymentStatus status,
            PaymentMethod paymentMethod,
            String externalReference) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.externalReference = externalReference;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
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

    public String getMpCheckoutUrl() {
        return mpCheckoutUrl;
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

    public void assignPreference(String mpPreferenceId, String mpCheckoutUrl) {
        this.mpPreferenceId = mpPreferenceId;
        this.mpCheckoutUrl = mpCheckoutUrl;
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

    private static BigDecimal inferUnitPrice(BigDecimal totalAmount, Integer quantity) {
        return totalAmount.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }
}
