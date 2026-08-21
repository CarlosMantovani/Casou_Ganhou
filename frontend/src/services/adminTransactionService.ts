import { apiClient } from '../config/apiClient';
import type {
  AdminTransactionResponse,
  AdminTransactionSummaryResponse,
  CashTransactionCreateRequest,
  CashTransactionCreateResponse,
} from '../types/admin';
import type { PageResponse } from '../types/page';

export interface AdminTransactionListParams {
  query?: string;
  page: number;
  size: number;
  sort?: string;
}

export const adminTransactionService = {
  async getSummary(): Promise<AdminTransactionSummaryResponse> {
    const response = await apiClient.get<AdminTransactionSummaryResponse>('/transactions/summary');
    return response.data;
  },

  async list(params: AdminTransactionListParams): Promise<PageResponse<AdminTransactionResponse>> {
    const response = await apiClient.get<PageResponse<AdminTransactionResponse>>('/transactions', {
      params: {
        query: params.query || undefined,
        page: params.page,
        size: params.size,
        sort: params.sort,
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
