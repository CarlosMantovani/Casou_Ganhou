import { apiClient } from '../config/apiClient';
import type {
  TransactionCreateRequest,
  TransactionCreateResponse,
  TransactionQuoteRequest,
  TransactionQuoteResponse,
  TransactionStatusResponse,
} from '../types/transaction';

export const transactionService = {
  async quote(request: TransactionQuoteRequest): Promise<TransactionQuoteResponse> {
    const response = await apiClient.post<TransactionQuoteResponse>('/transactions/quote', request);
    return response.data;
  },

  async create(request: TransactionCreateRequest): Promise<TransactionCreateResponse> {
    const response = await apiClient.post<TransactionCreateResponse>('/transactions', request);
    return response.data;
  },

  async getStatus(externalReference: string): Promise<TransactionStatusResponse> {
    const response = await apiClient.get<TransactionStatusResponse>(`/transactions/${externalReference}/status`);
    return response.data;
  },

  getLuckyNumbersPdfUrl(externalReference: string): string {
    return `${apiClient.defaults.baseURL}/transactions/${externalReference}/lucky-numbers.pdf`;
  },
};
