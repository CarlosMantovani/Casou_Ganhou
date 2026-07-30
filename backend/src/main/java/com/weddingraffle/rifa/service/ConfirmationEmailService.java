package com.weddingraffle.rifa.service;

public interface ConfirmationEmailService {

    void sendForApprovedTransaction(String externalReference);
}
