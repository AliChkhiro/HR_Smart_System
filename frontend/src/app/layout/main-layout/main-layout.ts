import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { NotificationsService } from '../../core/services/notifications.service';
import { NotificationDto, NotificationType } from '../../core/models/notification.model';

@Component({
  selector: 'app-main-layout',
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatTooltipModule,
    MatBadgeModule
  ],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss',
})
export class MainLayout {
  protected readonly authService = inject(AuthService);
  protected readonly router = inject(Router);
  private readonly notificationsService = inject(NotificationsService);

  protected readonly unreadCount = signal(0);
  protected readonly notifications = signal<NotificationDto[]>([]);

  protected readonly navItems = [
    { path: '/dashboard', icon: 'dashboard', label: 'Tableau de bord' },
    { path: '/projects', icon: 'folder', label: 'Projets' },
    { path: '/tasks', icon: 'checklist', label: 'Tâches & Kanban' },
    { path: '/calendar', icon: 'calendar_month', label: 'Calendrier' },
    { path: '/leaves', icon: 'beach_access', label: 'Congés' },
    { path: '/employees', icon: 'group', label: 'Employés' },
    { path: '/departments', icon: 'account_balance', label: 'Départements' },
    { path: '/users', icon: 'manage_accounts', label: 'Utilisateurs', adminOnly: true },
    { path: '/skills', icon: 'workspace_premium', label: 'Compétences' },
    { path: '/notifications', icon: 'notifications', label: 'Notifications' },
    { path: '/settings', icon: 'settings', label: 'Paramètres' }
  ];

  protected readonly visibleNavItems = signal(
    this.navItems.filter(item => !item.adminOnly || this.authService.isAdmin())
  );

  protected readonly user = this.authService.user;
  protected readonly appTitle = 'AppRH';

  constructor() {
    this.restoreSession();
    if (this.authService.isAuthenticated()) {
      this.loadNotifications();
    }
  }

  protected notificationIcon(type: NotificationType): string {
    switch (type) {
      case 'LEAVE_REQUEST':
        return 'beach_access';
      case 'LEAVE_REVIEW':
        return 'fact_check';
      default:
        return 'assignment_turned_in';
    }
  }

  protected openNotification(notification: NotificationDto): void {
    if (!notification.read) {
      this.notificationsService.markRead(notification.id).subscribe(() => this.loadNotifications());
    }
  }

  protected markAllNotificationsRead(): void {
    this.notificationsService.markAllRead().subscribe(() => this.loadNotifications());
  }

  private loadNotifications(): void {
    this.notificationsService.unreadCount().subscribe({
      next: count => this.unreadCount.set(count)
    });
    this.notificationsService.list(0, 8).subscribe({
      next: page => this.notifications.set(page.content)
    });
  }

  logout(): void {
    const refreshToken = this.authService.refreshToken;
    if (refreshToken) {
      this.authService.logout(refreshToken).subscribe();
    }
    this.authService.clearSession();
    this.router.navigate(['/login']);
  }

  private restoreSession(): void {
    if (this.authService.isAuthenticated() && !this.authService.user()) {
      this.authService.me().subscribe({
        next: user => this.authService.setSession({
          accessToken: this.authService.accessToken!,
          refreshToken: this.authService.refreshToken!,
          user
        }),
        error: () => {
          this.authService.clearSession();
          this.router.navigate(['/login']);
        }
      });
    }
  }
}
