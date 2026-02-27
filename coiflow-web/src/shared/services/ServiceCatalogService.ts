import axiosClient from '@/shared/api/axiosClient';
import { API_ENDPOINTS } from '@/config/api';
import type { ServiceItem, CreateServicePayload, UpdateServicePayload } from '@/shared/types/service';

export const ServiceCatalogService = {
  async getAll(): Promise<ServiceItem[]> {
    const response = await axiosClient.get<ServiceItem[]>(API_ENDPOINTS.SERVICES);
    return response.data;
  },

  async create(payload: CreateServicePayload): Promise<ServiceItem> {
    const response = await axiosClient.post<ServiceItem>(API_ENDPOINTS.SERVICES, payload);
    return response.data;
  },

  async update(id: string, payload: UpdateServicePayload): Promise<ServiceItem> {
    const response = await axiosClient.put<ServiceItem>(`${API_ENDPOINTS.SERVICES}/${id}`, payload);
    return response.data;
  },

  async remove(id: string): Promise<void> {
    await axiosClient.delete(`${API_ENDPOINTS.SERVICES}/${id}`);
  },
};
