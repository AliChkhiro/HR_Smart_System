import { Service } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

const TOKEN_KEY = 'apprh.access_token';
const USER_KEY = 'apprh.user';

@Service()
export class AuthService {
  private readonly http = inject(HttpClient);

  login(request: LoginRequest) {
    return this.http.post<{ accessToken: string; refreshToken: string; user: AuthUser }>(
      `${environment.apiUrl}/auth/login`,
      request
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  setSession(accessToken: string, refreshToken: string, user: AuthUser): void {
    localStorage.setItem(TOKEN_KEY, accessToken);
    localStorage.setItem('apprh.refresh_token', refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  get accessToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  get user(): AuthUser | null {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  }

  isAuthenticated(): boolean {
    return this.accessToken !== null;
  }
}
