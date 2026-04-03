import apiClient from './client';

export const dashboardApi = {
  getSummary: async () => {
    return apiClient.get('/dashboard');
  },
};
