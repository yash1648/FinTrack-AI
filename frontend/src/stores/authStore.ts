import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

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
  login: (resp: AuthResponse) => void;
  logout: () => void;
  setAccessToken: (token: string) => void;
  updateUser: (user: Partial<UserDto>) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
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
    }),
    {
      name: 'fintrack-auth',
      storage: createJSONStorage(() => localStorage),
      // We only want to persist the refreshToken and user info, not the accessToken
      partialize: (state) => ({
        refreshToken: state.refreshToken,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
