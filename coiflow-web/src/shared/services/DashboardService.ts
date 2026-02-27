import axiosClient from '@/shared/api/axiosClient';
import { API_ENDPOINTS } from '@/config/api';
import type { DashboardStats, DailyRevenue } from '@/shared/types/dashboard';

export const DashboardService = {
  async getStats(): Promise<DashboardStats> {
    const response = await axiosClient.get<DashboardStats>(API_ENDPOINTS.DASHBOARD.STATS);
    return response.data;
  },

  async getRevenue(start: string, end: string): Promise<DailyRevenue[]> {
    const response = await axiosClient.get<DailyRevenue[]>(API_ENDPOINTS.DASHBOARD.REVENUE, {
      params: { start, end },
    });
    return response.data;
  },
};
