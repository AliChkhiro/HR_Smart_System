import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Page } from '../models/user.model';
import { AuditLogDto, ErasureRequestDto, ErasureStatus, PersonalDataExport } from '../models/rgpd.model';

@Service()
export class RgpdService {
  private readonly http = inject(HttpClient);

  /** Droit d'accès : export JSON des données personnelles de l'utilisateur connecté. */
  exportPersonalData() {
    return this.http.get<PersonalDataExport>(`${environment.apiUrl}/rgpd/export`);
  }

  /** Droit à l'effacement : dépose une demande pour l'utilisateur connecté. */
  requestErasure(reason?: string) {
    return this.http.post<ErasureRequestDto>(`${environment.apiUrl}/rgpd/erasure-request`, {
      reason: reason?.trim() ? reason.trim() : null
    });
  }

  myErasureRequests() {
    return this.http.get<ErasureRequestDto[]>(`${environment.apiUrl}/rgpd/my-erasure-requests`);
  }

  listErasureRequests(status?: ErasureStatus, page = 0, size = 50) {
    const params: Record<string, string> = { page: String(page), size: String(size) };
    if (status) {
      params['status'] = status;
    }
    return this.http.get<Page<ErasureRequestDto>>(`${environment.apiUrl}/rgpd/erasure-requests`, { params });
  }

  approveErasure(id: number) {
    return this.http.post<ErasureRequestDto>(`${environment.apiUrl}/rgpd/erasure-requests/${id}/approve`, {});
  }

  rejectErasure(id: number, comment?: string) {
    return this.http.post<ErasureRequestDto>(`${environment.apiUrl}/rgpd/erasure-requests/${id}/reject`, {
      comment: comment?.trim() ? comment.trim() : null
    });
  }

  /** Consultation du journal d'audit (administrateur). */
  auditLog(page = 0, size = 50, entityType?: string, entityId?: string) {
    const params: Record<string, string> = { page: String(page), size: String(size) };
    if (entityType) {
      params['entityType'] = entityType;
    }
    if (entityId) {
      params['entityId'] = entityId;
    }
    return this.http.get<Page<AuditLogDto>>(`${environment.apiUrl}/audit`, { params });
  }
}
