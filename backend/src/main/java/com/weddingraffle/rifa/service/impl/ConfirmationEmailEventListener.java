package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.service.ConfirmationEmailService;
import com.weddingraffle.rifa.service.PaymentApprovedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ConfirmationEmailEventListener {

    private final ConfirmationEmailService confirmationEmailService;

    public ConfirmationEmailEventListener(ConfirmationEmailService confirmationEmailService) {
        this.confirmationEmailService = confirmationEmailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PaymentApprovedEvent event) {
        confirmationEmailService.sendForApprovedTransaction(event.externalReference());
    }
}
