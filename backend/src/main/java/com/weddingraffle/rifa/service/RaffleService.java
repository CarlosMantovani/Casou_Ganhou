package com.weddingraffle.rifa.service;

import com.weddingraffle.rifa.dto.RaffleCandidateResponse;
import com.weddingraffle.rifa.dto.RaffleDrawResponse;
import java.util.List;

public interface RaffleService {

    RaffleDrawResponse draw();

    RaffleDrawResponse getResult();

    List<RaffleCandidateResponse> listEligibleNumbers();
}
