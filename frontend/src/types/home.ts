export interface FlagRankingItem {
  code: string;
  name: string;
  emoji: string;
  totalNumbers: number;
}

export interface HomeSummaryResponse {
  scheduledDrawAt: string | null;
  flagRanking: FlagRankingItem[];
}
