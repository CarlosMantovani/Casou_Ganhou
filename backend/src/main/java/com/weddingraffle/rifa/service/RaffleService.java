package com.weddingraffle.rifa.service;

import com.weddingraffle.rifa.dto.RaffleDrawResponse;
import java.util.List;

public interface RaffleService {

    RaffleDrawResponse draw();

    RaffleDrawResponse getResult();

    List<String> listEligibleNumbers();
}
