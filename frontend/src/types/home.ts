export interface FlagRankingItem {
  code: string;
  name: string;
  emoji: string;
  position: number;
  progressPercent: number;
}

export interface HomeSummaryResponse {
  scheduledDrawAt: string | null;
  flagRanking: FlagRankingItem[];
  raffleResult: RaffleResult | null;
}

export interface RaffleResult {
  winningNumber: string;
  winnerName: string;
  drawnAt: string;
  participantFlagName: string | null;
  participantFlagEmoji: string | null;
}
