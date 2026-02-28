import axiosClient from '@/shared/api/axiosClient';
import { API_ENDPOINTS } from '@/config/api';
import type { Appointment, CreateAppointmentPayload, ReschedulePayload, AppointmentStatus } from '@/shared/types/appointment';

export const AppointmentService = {
  async getAll(start: string, end: string, barberId?: string): Promise<Appointment[]> {
    const params: Record<string, string> = { start, end };
    if (barberId) params.barberId = barberId;
    const response = await axiosClient.get<Appointment[]>(API_ENDPOINTS.APPOINTMENTS, { params });
    return response.data;
  },

  async getToCash(start: string, end: string): Promise<Appointment[]> {
    const response = await axiosClient.get<Appointment[]>(`${API_ENDPOINTS.APPOINTMENTS}/to-cash`, {
      params: { start, end },
    });
    return response.data;
  },

  async getById(id: string): Promise<Appointment> {
    const response = await axiosClient.get<Appointment>(`${API_ENDPOINTS.APPOINTMENTS}/${id}`);
    return response.data;
  },

  async create(payload: CreateAppointmentPayload): Promise<Appointment> {
    const response = await axiosClient.post<Appointment>(API_ENDPOINTS.APPOINTMENTS, payload);
    return response.data;
  },

  async updateStatus(id: string, status: AppointmentStatus): Promise<Appointment> {
    const response = await axiosClient.patch<Appointment>(`${API_ENDPOINTS.APPOINTMENTS}/${id}/status`, { status });
    return response.data;
  },

  async reschedule(id: string, payload: ReschedulePayload): Promise<Appointment> {
    const response = await axiosClient.patch<Appointment>(`${API_ENDPOINTS.APPOINTMENTS}/${id}/reschedule`, payload);
    return response.data;
  },
};
