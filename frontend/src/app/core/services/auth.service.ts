import { Service, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, UserDto } from '../models/user.model';

const TOKEN_KEY = 'apprh.access_token';
const REFRESH_KEY = 'apprh.refresh_token';
const USER_KEY = 'apprh.user';

@Service()
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly userSignal = signal<UserDto | null>(this.readStoredUser());
  readonly user = this.userSignal.asReadonly();
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');

  login(request: LoginRequest) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, request);
  }

  refresh(refreshToken: string) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/refresh`, { refreshToken });
  }

  me() {
    return this.http.get<UserDto>(`${environment.apiUrl}/auth/me`);
  }

  logout(refreshToken: string) {
    return this.http.post<void>(`${environment.apiUrl}/auth/logout`, { refreshToken });
  }

  setSession(session: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, session.accessToken);
    localStorage.setItem(REFRESH_KEY, session.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(session.user));
    this.userSignal.set(session.user);
  }

  clearSession(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    this.userSignal.set(null);
  }

  get accessToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  get refreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  isAuthenticated(): boolean {
    return this.accessToken !== null;
  }

  private readStoredUser(): UserDto | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as UserDto;
    } catch {
      localStorage.removeItem(USER_KEY);
      return null;
    }
  }
}
