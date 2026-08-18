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
  participantFlagName?: string | null;
  participantFlagEmoji?: string | null;
}

export interface RaffleCandidateResponse {
  luckyNumber: string;
  participantFlagName: string;
  participantFlagEmoji: string;
}

export interface RaffleConfigResponse {
  unitPrice: string;
  scheduledDrawAt: string | null;
  weddingProfile: WeddingProfile;
  updatedAt: string | null;
}

export interface UnitPriceUpdateRequest {
  unitPrice: string;
}

export interface ScheduledDrawAtUpdateRequest {
  scheduledDrawAt: string;
}

export interface WeddingPalette {
  ivory: string;
  ivoryDeep: string;
  ink: string;
  inkSoft: string;
  green: string;
  greenDeep: string;
  wine: string;
  gold: string;
  goldSoft: string;
  line: string;
}

export interface WeddingProfile {
  groomName: string;
  brideName: string;
  palette: WeddingPalette;
}

export type WeddingProfileUpdateRequest = WeddingProfile;
