export type PaymentStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface TransactionQuoteRequest {
  name: string;
  phone: string;
  email?: string | null;
  quantity: number;
}

export interface TransactionQuoteResponse {
  name: string;
  phone: string;
  email: string | null;
  quantity: number;
  unitPrice: string;
  totalAmount: string;
}

export interface TransactionCreateRequest {
  name: string;
  phone: string;
  email?: string | null;
  quantity: number;
}

export interface TransactionCreateResponse {
  externalReference: string;
  preferenceId: string;
  checkoutUrl: string;
}

export interface TransactionStatusResponse {
  externalReference: string;
  emailProvided: boolean;
  status: PaymentStatus;
  quantity: number;
  totalAmount: string;
  participantFlagName: string;
  participantFlagEmoji: string;
  luckyNumbers: string[];
}
