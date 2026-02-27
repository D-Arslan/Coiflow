import axiosClient from '@/shared/api/axiosClient';
import { API_ENDPOINTS } from '@/config/api';
import type { Transaction, CreateTransactionPayload } from '@/shared/types/transaction';

export const TransactionService = {
  async getAll(start: string, end: string): Promise<Transaction[]> {
    const response = await axiosClient.get<Transaction[]>(API_ENDPOINTS.TRANSACTIONS, {
      params: { start, end },
    });
    return response.data;
  },

  async getById(id: string): Promise<Transaction> {
    const response = await axiosClient.get<Transaction>(`${API_ENDPOINTS.TRANSACTIONS}/${id}`);
    return response.data;
  },

  async create(payload: CreateTransactionPayload): Promise<Transaction> {
    const response = await axiosClient.post<Transaction>(API_ENDPOINTS.TRANSACTIONS, payload);
    return response.data;
  },

  async voidTransaction(id: string): Promise<Transaction> {
    const response = await axiosClient.patch<Transaction>(`${API_ENDPOINTS.TRANSACTIONS}/${id}/void`);
    return response.data;
  },
};
