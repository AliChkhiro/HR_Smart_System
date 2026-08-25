import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { HolidayDto, HolidayType } from '../models/holiday.model';

@Service()
export class HolidaysService {
  private readonly http = inject(HttpClient);

  list(year?: number) {
    const params: Record<string, string> = {};
    if (year !== undefined) {
      params['year'] = String(year);
    }
    return this.http.get<HolidayDto[]>(`${environment.apiUrl}/holidays`, { params });
  }
}
