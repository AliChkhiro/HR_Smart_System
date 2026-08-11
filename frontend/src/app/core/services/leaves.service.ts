import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Page } from '../models/user.model';
import { LeaveCreateRequest, LeaveDto, LeaveReviewRequest, LeaveStatus, LeaveType } from '../models/leave.model';

@Service()
export class LeavesService {
  private readonly http = inject(HttpClient);

  list(employeeId?: number, status?: LeaveStatus, type?: LeaveType, from?: string, to?: string, page = 0, size = 100) {
    const params: Record<string, string> = { page: String(page), size: String(size) };
    if (employeeId !== undefined) {
      params['employeeId'] = String(employeeId);
    }
    if (status) {
      params['status'] = status;
    }
    if (type) {
      params['type'] = type;
    }
    if (from) {
      params['from'] = from;
    }
    if (to) {
      params['to'] = to;
    }
    return this.http.get<Page<LeaveDto>>(`${environment.apiUrl}/leaves`, { params });
  }

  create(request: LeaveCreateRequest) {
    return this.http.post<LeaveDto>(`${environment.apiUrl}/leaves`, request);
  }

  review(id: number, request: LeaveReviewRequest) {
    return this.http.patch<LeaveDto>(`${environment.apiUrl}/leaves/${id}/review`, request);
  }

  cancel(id: number) {
    return this.http.delete<void>(`${environment.apiUrl}/leaves/${id}`);
  }
}
