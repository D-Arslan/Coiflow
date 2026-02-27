import axiosClient from '@/shared/api/axiosClient';
import { API_ENDPOINTS } from '@/config/api';
import type { Commission } from '@/shared/types/commission';

export const CommissionService = {
  async getAll(start: string, end: string, barberId?: string): Promise<Commission[]> {
    const params: Record<string, string> = { start, end };
    if (barberId) params.barberId = barberId;
    const response = await axiosClient.get<Commission[]>(API_ENDPOINTS.COMMISSIONS, { params });
    return response.data;
  },
};
