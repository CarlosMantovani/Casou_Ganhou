import axios, { AxiosError } from 'axios';

import { env } from './env';
import { clearStoredAdminSession, getStoredAdminSession } from '../services/adminSession';
import type { ApiError, ApiErrorResponse } from '../types/api';

const DEFAULT_ERROR_MESSAGE = 'Não foi possível concluir a operação. Tente novamente em alguns instantes.';

export const apiClient = axios.create({
  baseURL: env.apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const session = getStoredAdminSession();

  if (session) {
    config.headers.Authorization = `${session.tokenType} ${session.accessToken}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    const data = error.response?.data;
    const apiError: ApiError = {
      code: data?.code ?? 'UNKNOWN_ERROR',
      message: data?.message ?? DEFAULT_ERROR_MESSAGE,
      status: error.response?.status,
      fieldErrors: data?.fieldErrors ?? [],
    };

    if ((apiError.status === 401 || apiError.status === 403) && window.location.pathname.startsWith('/admin')) {
      clearStoredAdminSession();
      window.history.replaceState({}, '', '/admin/login');
      window.dispatchEvent(new PopStateEvent('popstate'));
    }

    return Promise.reject(apiError);
  },
);
