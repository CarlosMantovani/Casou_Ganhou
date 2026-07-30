package com.weddingraffle.rifa.service;

import com.weddingraffle.rifa.dto.RaffleDrawResponse;

public interface RaffleService {

    RaffleDrawResponse draw();

    RaffleDrawResponse getResult();
}
