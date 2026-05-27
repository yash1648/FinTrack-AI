import apiClient from './client';
import type {
  ApiResponse,
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  RefreshTokenRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  ChangePasswordRequest,
  UpdateProfileRequest,
  UserDto,
  MessageResponse,
} from './types';

export const authApi = {
  login: async (data: LoginRequest): Promise<ApiResponse<AuthResponse>> => {
    return apiClient.post('/auth/login', data);
  },

  register: async (data: RegisterRequest): Promise<ApiResponse<MessageResponse>> => {
    return apiClient.post('/auth/register', data);
  },

  logout: async (refreshToken: string): Promise<ApiResponse<MessageResponse>> => {
    return apiClient.post('/auth/logout', { refreshToken } as RefreshTokenRequest);
  },

  getProfile: async (): Promise<ApiResponse<UserDto>> => {
    return apiClient.get('/auth/profile');
  },

  updateProfile: async (data: UpdateProfileRequest): Promise<ApiResponse<UserDto>> => {
    return apiClient.patch('/auth/profile', data);
  },

  changePassword: async (data: ChangePasswordRequest): Promise<ApiResponse<MessageResponse>> => {
    return apiClient.patch('/auth/change-password', data);
  },

  forgotPassword: async (email: string): Promise<ApiResponse<MessageResponse>> => {
    return apiClient.post('/auth/forgot-password', { email } as ForgotPasswordRequest);
  },

  resetPassword: async (data: ResetPasswordRequest): Promise<ApiResponse<MessageResponse>> => {
    return apiClient.post('/auth/reset-password', data);
  },

  verifyEmail: async (token: string): Promise<ApiResponse<MessageResponse>> => {
    return apiClient.get(`/auth/verify-email?token=${encodeURIComponent(token)}`);
  },
};
