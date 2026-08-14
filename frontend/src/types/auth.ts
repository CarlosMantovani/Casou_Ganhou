export interface AuthLoginRequest {
  username: string;
  password: string;
}

export interface AuthLoginResponse {
  tokenType: string;
  accessToken: string;
  expiresIn: number;
}

export interface AdminSession {
  tokenType: string;
  accessToken: string;
  expiresAt: number;
}
