import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { DashboardStats } from '../models/dashboard.model';

@Service()
export class DashboardService {
  private readonly http = inject(HttpClient);

  stats() {
    return this.http.get<DashboardStats>(`${environment.apiUrl}/dashboard/stats`);
  }
}