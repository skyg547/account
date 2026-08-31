export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthenticatedSession {
  expiresIn: number;
  username: string;
  departmentCode: string;
  roles: string[];
  roleVersion: number;
}

export const authService = {
  login: async (credentials: LoginRequest): Promise<AuthenticatedSession> => {
    const normalizedUsername = credentials.username.trim();
    if (!normalizedUsername || !credentials.password.trim()) {
      throw new Error('아이디와 비밀번호를 모두 입력해 주세요.');
    }

    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'same-origin',
      body: JSON.stringify({
        username: normalizedUsername,
        password: credentials.password,
      }),
    });

    if (!response.ok) {
      throw new Error('로그인에 실패했습니다.');
    }

    const data: AuthenticatedSession = await response.json();
    return data;
  },
};
