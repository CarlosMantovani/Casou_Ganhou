import { apiClient } from '../config/apiClient';
import type { RaffleDrawResponse } from '../types/admin';

export const raffleService = {
  async draw(): Promise<RaffleDrawResponse> {
    const response = await apiClient.post<RaffleDrawResponse>('/raffle/draw');
    return response.data;
  },

  async getResult(): Promise<RaffleDrawResponse> {
    const response = await apiClient.get<RaffleDrawResponse>('/raffle/result');
    return response.data;
  },
};
