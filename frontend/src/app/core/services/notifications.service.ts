import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Page } from '../models/user.model';
import { NotificationDto } from '../models/notification.model';

@Service()
export class NotificationsService {
  private readonly http = inject(HttpClient);

  list(page = 0, size = 20) {
    return this.http.get<Page<NotificationDto>>(`${environment.apiUrl}/notifications`, {
      params: { page: String(page), size: String(size) }
    });
  }

  unreadCount() {
    return this.http.get<number>(`${environment.apiUrl}/notifications/unread-count`);
  }

  markRead(id: number) {
    return this.http.patch<void>(`${environment.apiUrl}/notifications/${id}/read`, {});
  }

  markAllRead() {
    return this.http.patch<void>(`${environment.apiUrl}/notifications/read-all`, {});
  }
}
