export type PaymentStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface TransactionQuoteRequest {
  email: string;
  quantity: number;
}

export interface TransactionQuoteResponse {
  email: string;
  quantity: number;
  unitPrice: string;
  totalAmount: string;
}

export interface TransactionCreateRequest {
  email: string;
  quantity: number;
}

export interface TransactionCreateResponse {
  externalReference: string;
  preferenceId: string;
  checkoutUrl: string;
}

export interface TransactionStatusResponse {
  externalReference: string;
  status: PaymentStatus;
  quantity: number;
  totalAmount: string;
  luckyNumbers: string[];
}
