import { apiClient } from '../config/apiClient';
import type { FlagRankingItem, HomeSummaryResponse } from '../types/home';

export const homeService = {
  async getSummary(): Promise<HomeSummaryResponse> {
    const response = await apiClient.get<HomeSummaryResponse>('/public/home-summary');
    return response.data;
  },

  async getFlagRanking(): Promise<FlagRankingItem[]> {
    const response = await apiClient.get<FlagRankingItem[]>('/public/flag-ranking');
    return response.data;
  },
};
