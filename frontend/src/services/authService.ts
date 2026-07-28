export interface LoginRequest {
  username: string;
  password?: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  departmentCode: string;
  roles: string[];
  roleVersion: number;
}

const AUTH_API_BASE_URL = process.env.NEXT_PUBLIC_AUTH_API_URL || 'http://localhost:8080';

export const authService = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await fetch(`${AUTH_API_BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: credentials.username,
        password: credentials.password || '1234',
      }),
    });

    if (!response.ok) {
      throw new Error(`로그인 실패 (상태 코드: ${response.status})`);
    }

    const data: LoginResponse = await response.json();
    return data;
  },
};
