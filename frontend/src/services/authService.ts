import { apiClient } from '../config/apiClient';
import { createAdminSession, storeAdminSession } from './adminSession';
import type { AuthLoginRequest, AuthLoginResponse } from '../types/auth';

export const authService = {
  async login(request: AuthLoginRequest): Promise<AuthLoginResponse> {
    const response = await apiClient.post<AuthLoginResponse>('/auth/login', request);
    const session = createAdminSession(response.data);
    storeAdminSession(session);
    return response.data;
  },
};
