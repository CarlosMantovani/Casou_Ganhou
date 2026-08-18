import { apiClient } from '../config/apiClient';
import type {
  RaffleConfigResponse,
  ScheduledDrawAtUpdateRequest,
  UnitPriceUpdateRequest,
  WeddingProfileUpdateRequest,
} from '../types/admin';

export const raffleConfigService = {
  async getConfig(): Promise<RaffleConfigResponse> {
    const response = await apiClient.get<RaffleConfigResponse>('/admin/raffle-config');
    return response.data;
  },

  async updateUnitPrice(request: UnitPriceUpdateRequest): Promise<RaffleConfigResponse> {
    const response = await apiClient.put<RaffleConfigResponse>('/admin/raffle-config/unit-price', request);
    return response.data;
  },

  async updateScheduledDrawAt(request: ScheduledDrawAtUpdateRequest): Promise<RaffleConfigResponse> {
    const response = await apiClient.put<RaffleConfigResponse>('/admin/raffle-config/scheduled-at', request);
    return response.data;
  },

  async updateWeddingProfile(request: WeddingProfileUpdateRequest): Promise<RaffleConfigResponse> {
    const response = await apiClient.put<RaffleConfigResponse>('/admin/raffle-config/wedding-profile', request);
    return response.data;
  },
};
