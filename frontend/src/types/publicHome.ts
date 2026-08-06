export interface TopBuyer {
  avatarEmoji: string;
  avatarColor: string;
  quantity: number;
}

export interface HomeSummary {
  scheduledDrawAt: string | null;
  topBuyers: TopBuyer[];
}
