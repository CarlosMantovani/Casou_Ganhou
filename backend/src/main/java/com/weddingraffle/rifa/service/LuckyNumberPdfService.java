package com.weddingraffle.rifa.service;

public interface LuckyNumberPdfService {

    byte[] generate(String externalReference);

    byte[] generateForParticipant(String externalReference);
}
