import apiClient from './client';
import type { ApiResponse, ReportDistribution, MonthlyTrend, DailyTrend, ReportQueryParams } from './types';

export const reportsApi = {
  getDistribution: async (params: ReportQueryParams): Promise<ApiResponse<ReportDistribution[]>> => {
    return apiClient.get('/reports/distribution', { params });
  },

  getMonthlyTrend: async (params: ReportQueryParams): Promise<ApiResponse<MonthlyTrend[]>> => {
    return apiClient.get('/reports/monthly', { params });
  },

  getDailyTrend: async (params: ReportQueryParams): Promise<ApiResponse<DailyTrend[]>> => {
    return apiClient.get('/reports/daily', { params });
  },
};
