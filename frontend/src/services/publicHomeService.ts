import { apiClient } from '../config/apiClient';
import type { HomeSummary } from '../types/publicHome';

export const publicHomeService = {
  async getSummary(): Promise<HomeSummary> {
    const response = await apiClient.get<HomeSummary>('/public/home-summary');
    return response.data;
  },
};
