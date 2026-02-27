import axiosClient from '@/shared/api/axiosClient';
import { API_ENDPOINTS } from '@/config/api';
import type { Client, CreateClientPayload, UpdateClientPayload } from '@/shared/types/client';

export const ClientService = {
  async getAll(search?: string): Promise<Client[]> {
    const response = await axiosClient.get<Client[]>(API_ENDPOINTS.CLIENTS, {
      params: search ? { search } : undefined,
    });
    return response.data;
  },

  async create(payload: CreateClientPayload): Promise<Client> {
    const response = await axiosClient.post<Client>(API_ENDPOINTS.CLIENTS, payload);
    return response.data;
  },

  async update(id: string, payload: UpdateClientPayload): Promise<Client> {
    const response = await axiosClient.put<Client>(`${API_ENDPOINTS.CLIENTS}/${id}`, payload);
    return response.data;
  },
};
