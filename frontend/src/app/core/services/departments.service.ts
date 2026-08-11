import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Page } from '../models/user.model';
import { DepartmentDto, DepartmentRequest } from '../models/department.model';

@Service()
export class DepartmentsService {
  private readonly http = inject(HttpClient);

  list(search?: string, page = 0, size = 100) {
    const params: Record<string, string> = { page: String(page), size: String(size) };
    if (search) {
      params['search'] = search;
    }
    return this.http.get<Page<DepartmentDto>>(`${environment.apiUrl}/departments`, { params });
  }

  create(request: DepartmentRequest) {
    return this.http.post<DepartmentDto>(`${environment.apiUrl}/departments`, request);
  }

  update(id: number, request: DepartmentRequest) {
    return this.http.patch<DepartmentDto>(`${environment.apiUrl}/departments/${id}`, request);
  }

  delete(id: number) {
    return this.http.delete<void>(`${environment.apiUrl}/departments/${id}`);
  }
}
