import apiClient from './client';
import type { ApiResponse, NotificationResponse, NotificationQueryParams, PaginationDto } from './types';

export const notificationsApi = {
  getNotifications: async (
    params: NotificationQueryParams
  ): Promise<ApiResponse<NotificationResponse[]> & { pagination: PaginationDto }> => {
    return apiClient.get('/notifications', { params });
  },

  markAsRead: async (id: string): Promise<ApiResponse<NotificationResponse>> => {
    return apiClient.patch(`/notifications/${id}/read`);
  },

  markAllAsRead: async (): Promise<ApiResponse<{ message: string }>> => {
    return apiClient.patch('/notifications/read-all');
  },
};
