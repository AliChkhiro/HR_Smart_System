import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-main-layout',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatBadgeModule,
    MatMenuModule
  ],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss',
})
export class MainLayout {
  protected readonly navItems = [
    { path: '/dashboard', icon: 'dashboard', label: 'Tableau de bord' },
    { path: '/projects', icon: 'folder', label: 'Projets' },
    { path: '/tasks', icon: 'checklist', label: 'Tâches & Kanban' },
    { path: '/calendar', icon: 'calendar_month', label: 'Calendrier' },
    { path: '/leaves', icon: 'beach_access', label: 'Congés' },
    { path: '/employees', icon: 'group', label: 'Employés' },
    { path: '/skills', icon: 'workspace_premium', label: 'Compétences' },
    { path: '/notifications', icon: 'notifications', label: 'Notifications' },
    { path: '/settings', icon: 'settings', label: 'Paramètres' }
  ];

  protected readonly authService = inject(AuthService);
  protected readonly router = inject(Router);
  protected readonly user = signal(this.authService.user);
  protected readonly appTitle = 'AppRH';

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
