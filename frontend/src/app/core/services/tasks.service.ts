import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Page } from '../models/user.model';
import {
  RecommendationDto,
  TaskCreateRequest,
  TaskDto,
  TaskPriority,
  TaskStatus,
  TaskStatusRequest,
  TaskUpdateRequest
} from '../models/task.model';

@Service()
export class TasksService {
  private readonly http = inject(HttpClient);

  list(search?: string, projectId?: number, assigneeId?: number, status?: string, priority?: string,
       dueFrom?: string, dueTo?: string, page = 0, size = 100) {
    const params: Record<string, string> = { page: String(page), size: String(size) };
    if (search) {
      params['search'] = search;
    }
    if (projectId !== undefined) {
      params['projectId'] = String(projectId);
    }
    if (assigneeId !== undefined) {
      params['assigneeId'] = String(assigneeId);
    }
    if (status) {
      params['status'] = status;
    }
    if (priority) {
      params['priority'] = priority;
    }
    if (dueFrom) {
      params['dueFrom'] = dueFrom;
    }
    if (dueTo) {
      params['dueTo'] = dueTo;
    }
    return this.http.get<Page<TaskDto>>(`${environment.apiUrl}/tasks`, { params });
  }

  create(request: TaskCreateRequest) {
    return this.http.post<TaskDto>(`${environment.apiUrl}/tasks`, request);
  }

  update(id: number, request: TaskUpdateRequest) {
    return this.http.patch<TaskDto>(`${environment.apiUrl}/tasks/${id}`, request);
  }

  updateStatus(id: number, status: TaskStatus) {
    return this.http.patch<TaskDto>(`${environment.apiUrl}/tasks/${id}/status`, { status } satisfies TaskStatusRequest);
  }

  delete(id: number) {
    return this.http.delete<void>(`${environment.apiUrl}/tasks/${id}`);
  }

  recommend(skillIds: number[], startDate?: string, dueDate?: string, priority?: TaskPriority) {
    return this.http.post<RecommendationDto[]>(`${environment.apiUrl}/tasks/recommend`, {
      skillIds: skillIds.length > 0 ? skillIds : [],
      startDate: startDate || undefined,
      dueDate: dueDate || undefined,
      priority: priority || undefined
    });
  }
}
