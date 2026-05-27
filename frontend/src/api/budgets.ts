import apiClient from './client';
import type { ApiResponse, BudgetEnriched, CreateBudgetRequest } from './types';

export const budgetsApi = {
  getBudgets: async (params?: {
    month?: number;
    year?: number;
  }): Promise<ApiResponse<BudgetEnriched[]>> => {
    return apiClient.get('/budgets', { params });
  },

  createBudget: async (data: CreateBudgetRequest): Promise<ApiResponse<BudgetEnriched>> => {
    return apiClient.post('/budgets', data);
  },

  updateBudget: async (id: string, data: { limitAmount: number }): Promise<ApiResponse<BudgetEnriched>> => {
    return apiClient.patch(`/budgets/${id}`, data);
  },

  deleteBudget: async (id: string): Promise<void> => {
    return apiClient.delete(`/budgets/${id}`);
  },
};
