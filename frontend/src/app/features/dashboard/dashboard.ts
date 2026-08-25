import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DashboardService } from '../../core/services/dashboard.service';
import { AuthService } from '../../core/services/auth.service';
import { DashboardStats } from '../../core/models/dashboard.model';

@Component({
  selector: 'app-dashboard',
  imports: [
    DatePipe,
    MatCardModule,
    MatIconModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatTooltipModule
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly dashboardService = inject(DashboardService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly isManager = inject(AuthService).isManager;
  protected readonly stats = signal<DashboardStats | null>(null);
  protected readonly loading = signal(true);
  protected readonly statuses = [
    { key: 'TODO', label: 'À faire', color: 'accent' },
    { key: 'IN_PROGRESS', label: 'En cours', color: 'primary' },
    { key: 'BLOCKED', label: 'Bloqué', color: 'warn' },
    { key: 'DONE', label: 'Terminé', color: 'default' }
  ] as const;

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.dashboardService.stats().subscribe({
      next: stats => {
        this.stats.set(stats);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Impossible de charger le tableau de bord', 'Fermer', { duration: 4000 });
      }
    });
  }

  protected totalTasks(): number {
    const stats = this.stats();
    if (!stats) {
      return 0;
    }
    return Object.values(stats.tasksByStatus).reduce((sum, count) => sum + count, 0);
  }

  protected statusPercent(key: string): number {
    const stats = this.stats();
    const total = this.totalTasks();
    if (!stats || total === 0) {
      return 0;
    }
    return Math.round((stats.tasksByStatus[key] ?? 0) * 100 / total);
  }

  protected completionPercent(): number {
    const stats = this.stats();
    const total = this.totalTasks();
    if (!stats || total === 0) {
      return 0;
    }
    return Math.round(((stats.tasksByStatus['DONE'] ?? 0) * 100) / total);
  }
}
