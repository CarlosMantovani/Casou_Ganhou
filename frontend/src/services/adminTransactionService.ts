import { apiClient } from '../config/apiClient';
import type { AdminTransactionResponse } from '../types/admin';
import type { PageResponse } from '../types/page';

export interface AdminTransactionListParams {
  email?: string;
  page: number;
  size: number;
}

export const adminTransactionService = {
  async list(params: AdminTransactionListParams): Promise<PageResponse<AdminTransactionResponse>> {
    const response = await apiClient.get<PageResponse<AdminTransactionResponse>>('/transactions', {
      params: {
        email: params.email || undefined,
        page: params.page,
        size: params.size,
      },
    });

    return response.data;
  },
};
