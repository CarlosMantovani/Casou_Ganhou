import type { PaymentStatus } from './transaction';

export interface AdminTransactionResponse {
  externalReference: string;
  email: string;
  quantity: number;
  totalAmount: string;
  status: PaymentStatus;
  luckyNumbers: string[];
}

export interface RaffleDrawResponse {
  winningNumber: string;
  winnerEmail: string;
  drawnAt: string;
}
