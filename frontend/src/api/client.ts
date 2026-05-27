import axios from 'axios';
import { useAuthStore } from '@/stores/authStore';

// Maximum number of token refresh retries to prevent infinite loops
const MAX_REFRESH_RETRIES = 3;

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
  timeout: 30000,
});

// Track refresh attempts per request
interface RetryConfig {
  _retry?: boolean;
  _retryCount?: number;
}

// Request interceptor for adding the bearer token
apiClient.interceptors.request.use(
  (config) => {
    const { accessToken } = useAuthStore.getState();
    if (accessToken && config.headers) {
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
    const originalRequest = error.config as RetryConfig;

    // Prevent infinite retry loop — cap at MAX_REFRESH_RETRIES
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      (originalRequest._retryCount || 0) < MAX_REFRESH_RETRIES
    ) {
      originalRequest._retry = true;
      originalRequest._retryCount = (originalRequest._retryCount || 0) + 1;

      try {
        const { refreshToken, setAccessToken, logout } = useAuthStore.getState();
        if (!refreshToken) throw new Error('No refresh token available');

        // Call refresh endpoint using a fresh axios instance (no interceptor loops)
        const response = await axios.post(
          `${import.meta.env.VITE_API_URL || '/api/v1'}/auth/refresh`,
          { refreshToken },
          { timeout: 10000 }
        );
        const { accessToken } = response.data.data;

        setAccessToken(accessToken);
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        }

        return apiClient(originalRequest);
      } catch (refreshError) {
        // Only logout if we've exhausted retries or got a real auth failure
        useAuthStore.getState().logout();
        return Promise.reject(refreshError);
      }
    }

    // Extract error message from backend's ApiErrorResponse format
    const errorData = error.response?.data;
    if (errorData && errorData.error && errorData.error.message) {
      return Promise.reject(new Error(errorData.error.message));
    }
    return Promise.reject(errorData || error);
  }
);

export default apiClient;