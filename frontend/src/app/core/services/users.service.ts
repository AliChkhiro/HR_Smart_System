import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Page, UserCreateRequest, UserDto, UserUpdateRequest } from '../models/user.model';

@Service()
export class UsersService {
  private readonly http = inject(HttpClient);

  list(search?: string, role?: string, page = 0, size = 100, unassigned?: boolean, status?: string) {
    const params: Record<string, string> = { page: String(page), size: String(size) };
    if (search) {
      params['search'] = search;
    }
    if (role) {
      params['role'] = role;
    }
    if (unassigned) {
      params['unassigned'] = 'true';
    }
    if (status) {
      params['status'] = status;
    }
    return this.http.get<Page<UserDto>>(`${environment.apiUrl}/users`, { params });
  }

  create(request: UserCreateRequest) {
    return this.http.post<UserDto>(`${environment.apiUrl}/users`, request);
  }

  update(id: number, request: UserUpdateRequest) {
    return this.http.patch<UserDto>(`${environment.apiUrl}/users/${id}`, request);
  }

  approve(id: number) {
    return this.http.post<UserDto>(`${environment.apiUrl}/users/${id}/approve`, {});
  }

  reject(id: number) {
    return this.http.post<UserDto>(`${environment.apiUrl}/users/${id}/reject`, {});
  }

  delete(id: number) {
    return this.http.delete<void>(`${environment.apiUrl}/users/${id}`);
  }
}
