package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.service.RaffleWinnerSelector;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class SecureRaffleWinnerSelector implements RaffleWinnerSelector {

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public int selectIndex(int bound) {
        return secureRandom.nextInt(bound);
    }
}
