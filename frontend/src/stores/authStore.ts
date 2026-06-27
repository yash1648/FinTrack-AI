import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import axios from 'axios';

interface UserDto {
  id: string;
  name: string;
  email: string;
  currency: string;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: UserDto;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserDto | null;
  isAuthenticated: boolean;
  /** Whether the store has finished initializing (token refresh attempted on cold start) */
  isInitialized: boolean;
  /** Whether an initialization (token refresh) is in progress */
  isInitializing: boolean;
  login: (resp: AuthResponse) => void;
  logout: () => void;
  setAccessToken: (token: string) => void;
  updateUser: (user: Partial<UserDto>) => void;
  /** Called once on app mount to attempt a silent token refresh from the stored refreshToken */
  initAuth: () => Promise<void>;
}

const API_BASE = import.meta.env.VITE_API_URL || '/api/v1';

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      isInitialized: false,
      isInitializing: false,
      login: (resp: AuthResponse) =>
        set({
          accessToken: resp.accessToken,
          refreshToken: resp.refreshToken,
          user: resp.user,
          isAuthenticated: true,
        }),
      logout: () =>
        set({
          accessToken: null,
          refreshToken: null,
          user: null,
          isAuthenticated: false,
        }),
      setAccessToken: (token: string) => set({ accessToken: token }),
      updateUser: (userUpdates) =>
        set((state) => ({
          user: state.user ? { ...state.user, ...userUpdates } : null,
        })),
      initAuth: async () => {
        const { refreshToken, isInitialized } = get();
        if (isInitialized) return;

        set({ isInitializing: true });

        if (refreshToken) {
          try {
            const response = await axios.post(
              `${API_BASE}/auth/refresh`,
              { refreshToken },
              { timeout: 10000 }
            );
            const { accessToken: newAccessToken } = response.data.data;
            set({
              accessToken: newAccessToken,
              isAuthenticated: true,
              isInitialized: true,
              isInitializing: false,
            });
            return;
          } catch {
            // Refresh failed — clear stale auth
            set({
              accessToken: null,
              refreshToken: null,
              user: null,
              isAuthenticated: false,
            });
          }
        }

        set({
          isInitialized: true,
          isInitializing: false,
          isAuthenticated: false,
        });
      },
    }),
    {
      name: 'fintrack-auth',
      storage: createJSONStorage(() => localStorage),
      // Only persist refreshToken and user — accessToken is ephemeral
      partialize: (state) => ({
        refreshToken: state.refreshToken,
        user: state.user,
      }),
    }
  )
);
