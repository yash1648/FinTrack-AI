import apiClient from './client';

export const categoriesApi = {
  getCategories: async () => {
    return apiClient.get('/categories');
  },
  createCategory: async (data: any) => {
    return apiClient.post('/categories', data);
  },
  updateCategory: async (id: string, data: any) => {
    return apiClient.patch(`/categories/${id}`, data);
  },
  deleteCategory: async (id: string) => {
    return apiClient.delete(`/categories/${id}`);
  },
};
