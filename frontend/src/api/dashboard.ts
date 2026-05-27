import apiClient from './client';
import type { ApiResponse, DashboardSummary } from './types';

export const dashboardApi = {
  getSummary: async (): Promise<ApiResponse<DashboardSummary>> => {
    return apiClient.get('/dashboard');
  },
};
