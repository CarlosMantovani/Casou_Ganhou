import { apiClient } from '../config/apiClient';
import type {
  RaffleConfigResponse,
  RaffleConfigScheduledDrawAtRequest,
  RaffleConfigUnitPriceRequest,
} from '../types/admin';

export const raffleConfigService = {
  async getConfig(): Promise<RaffleConfigResponse> {
    const response = await apiClient.get<RaffleConfigResponse>('/admin/raffle-config');
    return response.data;
  },

  async updateUnitPrice(request: RaffleConfigUnitPriceRequest): Promise<RaffleConfigResponse> {
    const response = await apiClient.put<RaffleConfigResponse>('/admin/raffle-config/unit-price', request);
    return response.data;
  },

  async updateScheduledDrawAt(request: RaffleConfigScheduledDrawAtRequest): Promise<RaffleConfigResponse> {
    const response = await apiClient.put<RaffleConfigResponse>('/admin/raffle-config/scheduled-at', request);
    return response.data;
  },
};
