import apiClient from './client';
import type {
  ApiResponse,
  TransactionResponse,
  CreateTransactionRequest,
  UpdateTransactionRequest,
  TransactionQueryParams,
  PaginationDto,
} from './types';

export const transactionsApi = {
  getTransactions: async (
    params: TransactionQueryParams
  ): Promise<ApiResponse<TransactionResponse[]> & { pagination: PaginationDto }> => {
    return apiClient.get('/transactions', { params });
  },

  getTransaction: async (id: string): Promise<ApiResponse<TransactionResponse>> => {
    return apiClient.get(`/transactions/${id}`);
  },

  createTransaction: async (data: CreateTransactionRequest): Promise<ApiResponse<TransactionResponse>> => {
    return apiClient.post('/transactions', data);
  },

  updateTransaction: async (
    id: string,
    data: UpdateTransactionRequest
  ): Promise<ApiResponse<TransactionResponse>> => {
    return apiClient.patch(`/transactions/${id}`, data);
  },

  deleteTransaction: async (id: string): Promise<void> => {
    return apiClient.delete(`/transactions/${id}`);
  },
};
