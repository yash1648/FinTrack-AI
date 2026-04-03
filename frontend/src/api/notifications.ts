import apiClient from './client';

export const notificationsApi = {
  getNotifications: async (params: any) => {
    return apiClient.get('/notifications', { params });
  },
  markAsRead: async (id: string) => {
    return apiClient.patch(`/notifications/${id}/read`);
  },
  markAllAsRead: async () => {
    return apiClient.patch('/notifications/read-all');
  },
};
