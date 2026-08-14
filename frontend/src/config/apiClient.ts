import axios, { AxiosError } from 'axios';

import { env } from './env';
import type { ApiError, ApiErrorResponse } from '../types/api';

const DEFAULT_ERROR_MESSAGE = 'Não foi possível concluir a operação. Tente novamente em alguns instantes.';

export const apiClient = axios.create({
  baseURL: env.apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
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

    return Promise.reject(apiError);
  },
);
