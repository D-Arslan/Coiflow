export interface DashboardStats {
  revenueToday: number;
  appointmentsToday: number;
  appointmentsByStatus: Record<string, number>;
  activeBarbersCount: number;
}

export interface DailyRevenue {
  date: string;
  revenue: number;
  transactionCount: number;
}
