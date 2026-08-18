import { apiClient } from '../config/apiClient';
import type { AdminTransactionResponse, CashTransactionCreateRequest, CashTransactionCreateResponse } from '../types/admin';
import type { PageResponse } from '../types/page';

export interface AdminTransactionListParams {
  query?: string;
  page: number;
  size: number;
}

export const adminTransactionService = {
  async list(params: AdminTransactionListParams): Promise<PageResponse<AdminTransactionResponse>> {
    const response = await apiClient.get<PageResponse<AdminTransactionResponse>>('/transactions', {
      params: {
        query: params.query || undefined,
        page: params.page,
        size: params.size,
      },
    });

    return response.data;
  },

  async createCashTransaction(request: CashTransactionCreateRequest): Promise<CashTransactionCreateResponse> {
    const response = await apiClient.post<CashTransactionCreateResponse>('/transactions/cash', request);
    return response.data;
  },

  async deleteCashTransaction(externalReference: string): Promise<void> {
    await apiClient.delete(`/transactions/${externalReference}`);
  },
};
