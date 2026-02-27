import axiosClient from '@/shared/api/axiosClient';
import { API_ENDPOINTS } from '@/config/api';
import type { Staff, CreateStaffPayload, UpdateStaffPayload } from '@/shared/types/staff';

export const StaffService = {
  async getAll(): Promise<Staff[]> {
    const response = await axiosClient.get<Staff[]>(API_ENDPOINTS.STAFF);
    return response.data;
  },

  async create(payload: CreateStaffPayload): Promise<Staff> {
    const response = await axiosClient.post<Staff>(API_ENDPOINTS.STAFF, payload);
    return response.data;
  },

  async update(id: string, payload: UpdateStaffPayload): Promise<Staff> {
    const response = await axiosClient.put<Staff>(`${API_ENDPOINTS.STAFF}/${id}`, payload);
    return response.data;
  },

  async remove(id: string): Promise<void> {
    await axiosClient.delete(`${API_ENDPOINTS.STAFF}/${id}`);
  },
};
