package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.service.LuckyNumberCandidateGenerator;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class SecureLuckyNumberCandidateGenerator implements LuckyNumberCandidateGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public int nextInt(int minInclusive, int maxInclusive) {
        return secureRandom.nextInt(minInclusive, maxInclusive + 1);
    }
}
