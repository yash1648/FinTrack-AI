import apiClient from './client';
import type { ApiResponse, CategoryDTO, CreateCategoryRequest } from './types';

export const categoriesApi = {
  getCategories: async (): Promise<ApiResponse<CategoryDTO[]>> => {
    return apiClient.get('/categories');
  },

  createCategory: async (data: CreateCategoryRequest): Promise<ApiResponse<CategoryDTO>> => {
    return apiClient.post('/categories', data);
  },

  updateCategory: async (id: string, data: CreateCategoryRequest): Promise<ApiResponse<CategoryDTO>> => {
    return apiClient.patch(`/categories/${id}`, data);
  },

  deleteCategory: async (id: string): Promise<void> => {
    return apiClient.delete(`/categories/${id}`);
  },
};
