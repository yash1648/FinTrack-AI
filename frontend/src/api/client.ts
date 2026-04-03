import axios from 'axios';
import { useAuthStore } from '@/stores/authStore';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

// Request interceptor for adding the bearer token
apiClient.interceptors.request.use(
  (config) => {
    const { accessToken } = useAuthStore.getState();
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for handling 401 and token refresh
apiClient.interceptors.response.use(
  (response) => {
    // Backend uses the envelope pattern: { success: true, data: { ... }, pagination: { ... } }
    // We unwrap the data for easier use in TanStack Query
    if (response.data && response.data.success) {
      return response.data;
    }
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // If the error is 401 and not a retry, try to refresh the token
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const { refreshToken, setAccessToken, logout } = useAuthStore.getState();
        if (!refreshToken) throw new Error('No refresh token available');

        // Call refresh endpoint
        const response = await axios.post('/api/v1/auth/refresh', { refreshToken });
        const { accessToken } = response.data.data;

        setAccessToken(accessToken);
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;

        return apiClient(originalRequest);
      } catch (refreshError) {
        useAuthStore.getState().logout();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error.response?.data || error);
  }
);

export default apiClient;
