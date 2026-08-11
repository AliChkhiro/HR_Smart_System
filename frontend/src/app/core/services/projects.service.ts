import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Page } from '../models/user.model';
import {
  ProjectDetailDto,
  ProjectDto,
  ProjectMemberRequest,
  ProjectRequest,
  ProjectUpdateRequest
} from '../models/project.model';

@Service()
export class ProjectsService {
  private readonly http = inject(HttpClient);

  list(search?: string, status?: string, priority?: string, page = 0, size = 100) {
    const params: Record<string, string> = { page: String(page), size: String(size) };
    if (search) {
      params['search'] = search;
    }
    if (status) {
      params['status'] = status;
    }
    if (priority) {
      params['priority'] = priority;
    }
    return this.http.get<Page<ProjectDto>>(`${environment.apiUrl}/projects`, { params });
  }

  get(id: number) {
    return this.http.get<ProjectDetailDto>(`${environment.apiUrl}/projects/${id}`);
  }

  create(request: ProjectRequest) {
    return this.http.post<ProjectDto>(`${environment.apiUrl}/projects`, request);
  }

  update(id: number, request: ProjectUpdateRequest) {
    return this.http.patch<ProjectDto>(`${environment.apiUrl}/projects/${id}`, request);
  }

  delete(id: number) {
    return this.http.delete<void>(`${environment.apiUrl}/projects/${id}`);
  }

  addMember(id: number, request: ProjectMemberRequest) {
    return this.http.post<ProjectDetailDto>(`${environment.apiUrl}/projects/${id}/members`, request);
  }

  updateMember(id: number, employeeId: number, request: ProjectMemberRequest) {
    return this.http.patch<ProjectDetailDto>(`${environment.apiUrl}/projects/${id}/members/${employeeId}`, request);
  }

  removeMember(id: number, employeeId: number) {
    return this.http.delete<ProjectDetailDto>(`${environment.apiUrl}/projects/${id}/members/${employeeId}`);
  }
}
