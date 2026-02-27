import { useQuery } from '@tanstack/react-query';
import { DashboardService } from '@/shared/services/DashboardService';

const DASHBOARD_KEY = ['dashboard'];

export function useDashboardStats() {
  return useQuery({
    queryKey: [...DASHBOARD_KEY, 'stats'],
    queryFn: () => DashboardService.getStats(),
    refetchInterval: 60_000,
  });
}

export function useDashboardRevenue(start: string, end: string) {
  return useQuery({
    queryKey: [...DASHBOARD_KEY, 'revenue', start, end],
    queryFn: () => DashboardService.getRevenue(start, end),
  });
}
