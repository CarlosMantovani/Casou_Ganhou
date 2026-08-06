import type { PaymentStatus } from './transaction';

export type PaymentMethod = 'MERCADO_PAGO' | 'CASH';

export interface AdminTransactionResponse {
  externalReference: string;
  name: string;
  phone: string;
  email: string | null;
  paymentMethod: PaymentMethod;
  quantity: number;
  totalAmount: string;
  unitPrice: string;
  status: PaymentStatus;
  createdAt: string | null;
  luckyNumbers: string[];
}

export interface CashTransactionCreateRequest {
  name: string;
  phone: string;
  email?: string;
  quantity: number;
}

export type CashTransactionCreateResponse = AdminTransactionResponse;

export interface RaffleDrawResponse {
  winningNumber: string;
  winnerName: string;
  drawnAt: string;
}

export interface RaffleConfigResponse {
  unitPrice: string;
  scheduledDrawAt: string | null;
}

export interface RaffleConfigUnitPriceRequest {
  unitPrice: string;
}

export interface RaffleConfigScheduledDrawAtRequest {
  scheduledDrawAt: string | null;
}
