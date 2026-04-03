import apiClient from './client';

export const authApi = {
  login: async (data: any) => {
    return apiClient.post('/auth/login', data);
  },
  register: async (data: any) => {
    return apiClient.post('/auth/register', data);
  },
  logout: async (refreshToken: string) => {
    return apiClient.post('/auth/logout', { refreshToken });
  },
  getProfile: async () => {
    return apiClient.get('/auth/profile');
  },
  updateProfile: async (data: any) => {
    return apiClient.patch('/auth/profile', data);
  },
  changePassword: async (data: any) => {
    return apiClient.patch('/auth/change-password', data);
  },
  forgotPassword: async (email: string) => {
    return apiClient.post('/auth/forgot-password', { email });
  },
  resetPassword: async (data: any) => {
    return apiClient.post('/auth/reset-password', data);
  },
  verifyEmail: async (token: string) => {
    return apiClient.get(`/auth/verify-email?token=${token}`);
  },
};
