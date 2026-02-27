import axiosClient from '@/shared/api/axiosClient';
import { API_ENDPOINTS } from '@/config/api';
import type { LoginRequest, UserInfo } from '@/shared/types/auth';

export const AuthService = {
  async login(data: LoginRequest): Promise<UserInfo> {
    const response = await axiosClient.post<UserInfo>(API_ENDPOINTS.AUTH.LOGIN, data);
    return response.data;
  },

  async logout(): Promise<void> {
    await axiosClient.post(API_ENDPOINTS.AUTH.LOGOUT);
  },

  async refresh(): Promise<UserInfo> {
    const response = await axiosClient.post<UserInfo>(API_ENDPOINTS.AUTH.REFRESH);
    return response.data;
  },

  async me(): Promise<UserInfo | null> {
    const response = await axiosClient.get<UserInfo>(API_ENDPOINTS.AUTH.ME);
    return response.status === 204 ? null : response.data;
  },
};
