import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Page } from '../models/user.model';
import { SkillDto, SkillRequest } from '../models/skill.model';

@Service()
export class SkillsService {
  private readonly http = inject(HttpClient);

  list(search?: string, category?: string, page = 0, size = 100) {
    const params: Record<string, string> = { page: String(page), size: String(size) };
    if (search) {
      params['search'] = search;
    }
    if (category) {
      params['category'] = category;
    }
    return this.http.get<Page<SkillDto>>(`${environment.apiUrl}/skills`, { params });
  }

  create(request: SkillRequest) {
    return this.http.post<SkillDto>(`${environment.apiUrl}/skills`, request);
  }

  update(id: number, request: SkillRequest) {
    return this.http.patch<SkillDto>(`${environment.apiUrl}/skills/${id}`, request);
  }

  delete(id: number) {
    return this.http.delete<void>(`${environment.apiUrl}/skills/${id}`);
  }
}
