import apiClient from './client';

export const budgetsApi = {
  getBudgets: async (params: any) => {
    return apiClient.get('/budgets', { params });
  },
  createBudget: async (data: any) => {
    return apiClient.post('/budgets', data);
  },
  updateBudget: async (id: string, data: any) => {
    return apiClient.patch(`/budgets/${id}`, data);
  },
  deleteBudget: async (id: string) => {
    return apiClient.delete(`/budgets/${id}`);
  },
};
