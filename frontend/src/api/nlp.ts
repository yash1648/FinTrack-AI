import apiClient from './client';

export const nlpApi = {
  parse: async (text: string) => {
    return apiClient.post('/nlp/parse', { text });
  },
};
