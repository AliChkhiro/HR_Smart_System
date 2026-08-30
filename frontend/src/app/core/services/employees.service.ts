import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Page } from '../models/user.model';
import { EmployeeCreateRequest, EmployeeDto, EmployeeSkillRequest, EmployeeUpdateRequest } from '../models/employee.model';

@Service()
export class EmployeesService {
  private readonly http = inject(HttpClient);

  list(search?: string, departmentId?: number, status?: string, page = 0, size = 100) {
    const params: Record<string, string> = { page: String(page), size: String(size) };
    if (search) {
      params['search'] = search;
    }
    if (departmentId !== undefined) {
      params['departmentId'] = String(departmentId);
    }
    if (status) {
      params['status'] = status;
    }
    return this.http.get<Page<EmployeeDto>>(`${environment.apiUrl}/employees`, { params });
  }

  me() {
    return this.http.get<EmployeeDto>(`${environment.apiUrl}/employees/me`);
  }

  create(request: EmployeeCreateRequest) {
    return this.http.post<EmployeeDto>(`${environment.apiUrl}/employees`, request);
  }

  update(id: number, request: EmployeeUpdateRequest) {
    return this.http.patch<EmployeeDto>(`${environment.apiUrl}/employees/${id}`, request);
  }

  delete(id: number) {
    return this.http.delete<void>(`${environment.apiUrl}/employees/${id}`);
  }

  addSkill(id: number, request: EmployeeSkillRequest) {
    return this.http.post<EmployeeDto>(`${environment.apiUrl}/employees/${id}/skills`, request);
  }

  updateSkill(id: number, skillId: number, request: EmployeeSkillRequest) {
    return this.http.patch<EmployeeDto>(`${environment.apiUrl}/employees/${id}/skills/${skillId}`, request);
  }

  removeSkill(id: number, skillId: number) {
    return this.http.delete<EmployeeDto>(`${environment.apiUrl}/employees/${id}/skills/${skillId}`);
  }
}
