import apiClient from './client';

export const analysisApi = {
  getInsights: async () => {
    return apiClient.get('/analysis/insights');
  },
  getAnomalies: async () => {
    return apiClient.get('/analysis/anomalies');
  },
  getProjection: async () => {
    return apiClient.get('/analysis/projection');
  },
};
