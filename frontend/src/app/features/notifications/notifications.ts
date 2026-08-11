import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NotificationsService } from '../../core/services/notifications.service';
import { NotificationDto, NotificationType } from '../../core/models/notification.model';

@Component({
  selector: 'app-notifications',
  imports: [DatePipe, MatCardModule, MatButtonModule, MatIconModule, MatListModule, MatProgressSpinnerModule],
  templateUrl: './notifications.html',
  styleUrl: './notifications.scss',
})
export class Notifications {
  private readonly notificationsService = inject(NotificationsService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly typeIcons: Record<NotificationType, string> = {
    LEAVE_REQUEST: 'beach_access',
    LEAVE_REVIEW: 'fact_check',
    TASK_ASSIGNED: 'assignment_turned_in'
  };
  protected readonly notifications = signal<NotificationDto[]>([]);
  protected readonly loading = signal(false);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.notificationsService.list(0, 50).subscribe({
      next: page => {
        this.notifications.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Impossible de charger les notifications', 'Fermer', { duration: 4000 });
      }
    });
  }

  protected markRead(notification: NotificationDto): void {
    if (notification.read) {
      return;
    }
    this.notificationsService.markRead(notification.id).subscribe({
      next: () => {
        notification.read = true;
        this.notifications.set([...this.notifications()]);
      }
    });
  }

  protected markAllRead(): void {
    this.notificationsService.markAllRead().subscribe({
      next: () => {
        this.notifications.update(list => list.map(n => ({ ...n, read: true })));
      }
    });
  }
}
