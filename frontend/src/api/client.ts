import axios, { InternalAxiosRequestConfig } from 'axios';
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
interface RetryConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
  _retryCount?: number;
}

// Track endpoints that should NOT auto-refresh on 401 (e.g. auth endpoints)
const AUTH_ENDPOINTS = ['/auth/login', '/auth/register', '/auth/refresh'];

function isAuthEndpoint(url?: string): boolean {
  if (!url) return false;
  return AUTH_ENDPOINTS.some((ep) => url.includes(ep));
}

// Request interceptor for adding the bearer token
apiClient.interceptors.request.use(
  (config) => {
    const { accessToken } = useAuthStore.getState();
    // Only add the header if we actually have a token (avoid "Bearer null")
    if (accessToken && config.headers) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

/**
 * Attempt to extract a human-readable message from any backend error format.
 * Backend formats:
 *   ApiErrorResponse: { success: false, error: { code, message, fields? } }
 *   Spring defaults:  { error, message, path, status, timestamp }
 *   HTML:             response statusText
 */
function extractErrorMessage(error: unknown): string {
  const err = error as any;
  const data = err?.response?.data;

  if (!data) {
    return err?.message || 'An unexpected error occurred';
  }

  // Our ApiErrorResponse format
  if (data.error?.message) {
    return data.error.message;
  }

  // Spring Boot default error format
  if (data.message && typeof data.message === 'string') {
    return data.message;
  }

  // Axios error message
  if (err?.message) {
    return err.message;
  }

  return 'An unexpected error occurred';
}

// Response interceptor for handling 401 and token refresh
apiClient.interceptors.response.use(
  (response) => {
    // Backend uses the envelope pattern: { success: true, data: { ... }, pagination: { ... } }
    // For void responses (204 No Content), return as-is
    if (response.status === 204 || !response.data) {
      return response;
    }
    // Unwrap the envelope for easier use in TanStack Query
    if (response.data && typeof response.data === 'object' && 'success' in response.data && response.data.success) {
      return response.data;
    }
    return response;
  },
  async (error) => {
    const originalRequest = error.config as RetryConfig;

    // Don't try to refresh on auth endpoints themselves (avoid loops)
    if (isAuthEndpoint(originalRequest.url)) {
      return Promise.reject(new Error(extractErrorMessage(error)));
    }

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
        if (!refreshToken) {
          logout();
          return Promise.reject(new Error('Session expired. Please log in again.'));
        }

        // Call refresh endpoint using a fresh axios instance (no interceptor loops)
        const response = await axios.post(
          `${import.meta.env.VITE_API_URL || '/api/v1'}/auth/refresh`,
          { refreshToken },
          { timeout: 10000 }
        );
        const { accessToken: newAccessToken } = response.data.data;

        setAccessToken(newAccessToken);
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        }

        return apiClient(originalRequest);
      } catch (refreshError) {
        // Refresh failed — session is truly dead
        useAuthStore.getState().logout();
        return Promise.reject(new Error('Session expired. Please log in again.'));
      }
    }

    return Promise.reject(new Error(extractErrorMessage(error)));
  }
);

export default apiClient;