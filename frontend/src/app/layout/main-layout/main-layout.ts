import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterOutlet, NavigationEnd } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { CommonModule, DatePipe } from '@angular/common';
import { filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { NotificationsService } from '../../core/services/notifications.service';
import { NotificationDto, NotificationType } from '../../core/models/notification.model';
import { UserRole } from '../../core/models/user.model';

interface NavItem {
  path: string;
  icon: string;
  label: string;
  section: string;
  adminOnly?: boolean;
}

@Component({
  selector: 'app-main-layout',
  imports: [
    CommonModule,
    DatePipe,
    RouterOutlet,
    RouterLink,
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
  protected readonly collapsed = signal(false);

  protected readonly navItems: NavItem[] = [
    { path: '/dashboard', icon: 'dashboard', label: 'Tableau de bord', section: 'GÉNÉRAL' },
    { path: '/projects', icon: 'folder', label: 'Projets', section: 'GÉNÉRAL' },
    { path: '/tasks', icon: 'checklist', label: 'Tâches & Kanban', section: 'GÉNÉRAL' },
    { path: '/calendar', icon: 'calendar_month', label: 'Calendrier', section: 'GÉNÉRAL' },
    { path: '/leaves', icon: 'beach_access', label: 'Congés', section: 'RESSOURCES HUMAINES' },
    { path: '/employees', icon: 'group', label: 'Employés', section: 'RESSOURCES HUMAINES' },
    { path: '/departments', icon: 'account_balance', label: 'Départements', section: 'RESSOURCES HUMAINES' },
    { path: '/skills', icon: 'workspace_premium', label: 'Compétences', section: 'RESSOURCES HUMAINES' },
    { path: '/notifications', icon: 'notifications', label: 'Notifications', section: 'RESSOURCES HUMAINES' },
    { path: '/users', icon: 'manage_accounts', label: 'Utilisateurs', section: 'SYSTÈME', adminOnly: true },
    { path: '/settings', icon: 'settings', label: 'Paramètres', section: 'SYSTÈME' }
  ];

  protected readonly visibleNavItems = signal(
    this.navItems.filter(item => !item.adminOnly || this.authService.isAdmin())
  );

  protected readonly currentPageLabel = signal('');
  protected readonly user = this.authService.user;
  protected readonly appTitle = 'AppRH';

  constructor() {
    this.restoreSession();
    if (this.authService.isAuthenticated()) {
      this.loadNotifications();
    }
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => this.updatePageLabel());
    this.updatePageLabel();
  }

  toggleSidebar(): void {
    this.collapsed.update(value => !value);
  }

  protected isActive(path: string): boolean {
    return this.router.url === path || this.router.url.startsWith(path + '/');
  }

  protected initials(user: { firstName: string; lastName: string }): string {
    const first = user.firstName.charAt(0) ?? '';
    const last = user.lastName.charAt(0) ?? '';
    return (first + last).toUpperCase();
  }

  protected roleLabel(role: UserRole): string {
    switch (role) {
      case 'ADMIN':
        return 'Administrateur';
      case 'RH':
        return 'Responsable RH';
      case 'CHEF_PROJET':
        return 'Chef de projet';
      default:
        return 'Employé';
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
      this.notificationsService.markRead(notification.id).subscribe(() => {
        this.loadNotifications();
        this.router.navigate(['/notifications']);
      });
    } else {
      this.router.navigate(['/notifications']);
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

  private updatePageLabel(): void {
    const match = this.navItems.find(item => this.isActive(item.path));
    this.currentPageLabel.set(match ? match.label : this.appTitle);
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
