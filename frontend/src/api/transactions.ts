import apiClient from './client';

export const transactionsApi = {
  getTransactions: async (params: any) => {
    return apiClient.get('/transactions', { params });
  },
  getTransaction: async (id: string) => {
    return apiClient.get(`/transactions/${id}`);
  },
  createTransaction: async (data: any) => {
    return apiClient.post('/transactions', data);
  },
  updateTransaction: async (id: string, data: any) => {
    return apiClient.patch(`/transactions/${id}`, data);
  },
  deleteTransaction: async (id: string) => {
    return apiClient.delete(`/transactions/${id}`);
  },
};
