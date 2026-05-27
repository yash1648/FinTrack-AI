import apiClient from './client';
import type { ApiResponse, ParseResponse } from './types';

export const nlpApi = {
  parse: async (text: string): Promise<ApiResponse<ParseResponse>> => {
    return apiClient.post('/nlp/parse', { text });
  },
};
