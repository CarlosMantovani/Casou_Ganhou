package com.weddingraffle.rifa.dto;

import com.weddingraffle.rifa.entity.PaymentStatus;

public enum PaymentStatusResponse {
    PENDENTE,
    APROVADO,
    REJEITADO,
    CANCELADO,
    ESTORNADO,
    CHARGEBACK,
    EM_MEDIACAO;

    public static PaymentStatusResponse from(PaymentStatus status) {
        return switch (status) {
            case PENDING -> PENDENTE;
            case APPROVED -> APROVADO;
            case REJECTED -> REJEITADO;
            case CANCELLED -> CANCELADO;
            case REFUNDED -> ESTORNADO;
            case CHARGED_BACK -> CHARGEBACK;
            case IN_MEDIATION -> EM_MEDIACAO;
        };
    }
}
