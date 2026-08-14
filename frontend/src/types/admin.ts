import type { PaymentStatus } from './transaction';

export type PaymentMethod = 'MERCADO_PAGO' | 'CASH';

export interface AdminTransactionResponse {
  externalReference: string;
  createdAt: string;
  name: string;
  phone: string;
  email: string | null;
  paymentMethod: PaymentMethod;
  quantity: number;
  totalAmount: string;
  status: PaymentStatus;
  luckyNumbers: string[];
}

export interface CashTransactionCreateRequest {
  name: string;
  phone: string;
  email?: string;
  quantity: number;
}

export interface CashTransactionCreateResponse {
  externalReference: string;
  name: string;
  phone: string;
  email: string | null;
  paymentMethod: PaymentMethod;
  quantity: number;
  totalAmount: string;
  status: PaymentStatus;
  luckyNumbers: string[];
}

export interface RaffleDrawResponse {
  winningNumber: string;
  winnerName: string;
  drawnAt: string;
}
