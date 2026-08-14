export interface FieldErrorResponse {
  field: string;
  message: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  fieldErrors: FieldErrorResponse[];
}

export interface ApiError {
  code: string;
  message: string;
  status?: number;
  fieldErrors: FieldErrorResponse[];
}
