import { apiClient } from '../config/apiClient';
import type { RaffleConfigResponse, UnitPriceUpdateRequest } from '../types/admin';

export const raffleConfigService = {
  async getConfig(): Promise<RaffleConfigResponse> {
    const response = await apiClient.get<RaffleConfigResponse>('/admin/raffle-config');
    return response.data;
  },

  async updateUnitPrice(request: UnitPriceUpdateRequest): Promise<RaffleConfigResponse> {
    const response = await apiClient.put<RaffleConfigResponse>('/admin/raffle-config/unit-price', request);
    return response.data;
  },
};
