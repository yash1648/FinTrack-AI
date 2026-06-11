import apiClient from './client';
import type { ApiResponse, ReportDistribution, MonthlyTrend, DailyTrend, ReportQueryParams, ReportSummary } from './types';

export const reportsApi = {
  getDistribution: async (params: ReportQueryParams): Promise<ApiResponse<ReportDistribution[]>> => {
    return apiClient.get('/reports/category-distribution', { params });
  },

  getMonthlyTrend: async (params: ReportQueryParams): Promise<ApiResponse<MonthlyTrend[]>> => {
    return apiClient.get('/reports/monthly-comparison', { params });
  },

  getDailyTrend: async (params: ReportQueryParams): Promise<ApiResponse<DailyTrend[]>> => {
    return apiClient.get('/reports/daily-spending', { params });
  },

  getSummary: async (params: ReportQueryParams): Promise<ApiResponse<ReportSummary>> => {
    return apiClient.get('/reports/summary', { params });
  },

  exportCsv: async (params: ReportQueryParams): Promise<Blob> => {
    const response = await apiClient.get('/reports/export', {
      params,
      responseType: 'blob',
    });
    return response as unknown as Blob;
  },
};
