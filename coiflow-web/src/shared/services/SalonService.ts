import axiosClient from '@/shared/api/axiosClient';
import { API_ENDPOINTS } from '@/config/api';
import type { Salon, CreateSalonPayload, UpdateSalonPayload } from '@/shared/types/salon';

export const SalonService = {
  async getAll(): Promise<Salon[]> {
    const response = await axiosClient.get<Salon[]>(API_ENDPOINTS.ADMIN.SALONS);
    return response.data;
  },

  async create(payload: CreateSalonPayload): Promise<Salon> {
    const response = await axiosClient.post<Salon>(API_ENDPOINTS.ADMIN.SALONS, payload);
    return response.data;
  },

  async update(id: string, payload: UpdateSalonPayload): Promise<Salon> {
    const response = await axiosClient.put<Salon>(`${API_ENDPOINTS.ADMIN.SALONS}/${id}`, payload);
    return response.data;
  },

  async toggle(id: string): Promise<Salon> {
    const response = await axiosClient.patch<Salon>(`${API_ENDPOINTS.ADMIN.SALONS}/${id}/toggle`);
    return response.data;
  },
};
