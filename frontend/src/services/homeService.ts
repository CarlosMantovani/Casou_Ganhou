import { apiClient } from '../config/apiClient';
import type { HomeSummaryResponse } from '../types/home';

export const homeService = {
  async getSummary(): Promise<HomeSummaryResponse> {
    const response = await apiClient.get<HomeSummaryResponse>('/public/home-summary');
    return response.data;
  },
};
