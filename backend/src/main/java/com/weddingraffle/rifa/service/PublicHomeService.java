package com.weddingraffle.rifa.service;

import com.weddingraffle.rifa.dto.FlagRankingResponse;
import com.weddingraffle.rifa.dto.HomeSummaryResponse;
import java.util.List;

public interface PublicHomeService {

    HomeSummaryResponse getSummary();

    List<FlagRankingResponse> getFlagRanking();
}
