import apiClient from './client';

export const reportsApi = {
  getDistribution: async (params: any) => {
    return apiClient.get('/reports/distribution', { params });
  },
  getMonthlyTrend: async (params: any) => {
    return apiClient.get('/reports/monthly', { params });
  },
  getDailyTrend: async (params: any) => {
    return apiClient.get('/reports/daily', { params });
  },
};
