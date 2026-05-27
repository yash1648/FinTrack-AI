import apiClient from './client';
import type { ApiResponse, InsightData, AnomalyResponse, ProjectionResponse } from './types';

export const analysisApi = {
  getInsights: async (): Promise<ApiResponse<InsightData>> => {
    return apiClient.get('/analysis/insights');
  },

  getAnomalies: async (): Promise<ApiResponse<AnomalyResponse[]>> => {
    return apiClient.get('/analysis/anomalies');
  },

  getProjection: async (): Promise<ApiResponse<ProjectionResponse>> => {
    return apiClient.get('/analysis/projection');
  },
};
