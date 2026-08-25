import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { LeavesService } from '../../core/services/leaves.service';
import { AuthService } from '../../core/services/auth.service';
import { LeaveDto, LeaveStatus } from '../../core/models/leave.model';
import { LeaveDialogComponent } from './leave-dialog';

@Component({
  selector: 'app-leaves',
  imports: [
    DatePipe,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    FormsModule
  ],
  templateUrl: './leaves.html',
  styleUrl: './leaves.scss',
})
export class Leaves {
  private readonly leavesService = inject(LeavesService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly isManager = inject(AuthService).isManager;
  protected readonly typeLabels: Record<string, string> = {
    ANNUAL: 'Annuel',
    SICK: 'Maladie',
    MATERNITY: 'Maternité',
    PATERNITY: 'Paternité',
    UNPAID: 'Sans solde',
    OTHER: 'Autre'
  };
  protected readonly statusLabels: Record<string, string> = {
    PENDING: 'En attente',
    APPROVED: 'Approuvé',
    REJECTED: 'Rejeté',
    CANCELLED: 'Annulé'
  };
  protected readonly statusColors: Record<string, 'accent' | 'primary' | 'warn' | 'default'> = {
    PENDING: 'accent',
    APPROVED: 'primary',
    REJECTED: 'warn',
    CANCELLED: 'default'
  };
  protected readonly columns: string[] = [
    'employee', 'type', 'period', 'days', 'status', 'review', 'actions'
  ];

  protected readonly leaves = signal<LeaveDto[]>([]);
  protected readonly loading = signal(false);
  protected statusFilter = '';
  protected typeFilter = '';

  protected statusChipClass(status: string): string {
    switch (status) {
      case 'APPROVED':
        return 'chip-green';
      case 'REJECTED':
        return 'chip-red';
      case 'CANCELLED':
        return 'chip-gray';
      default:
        return 'chip-amber';
    }
  }

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.leavesService.list(
      undefined,
      this.statusFilter as LeaveStatus | undefined,
      this.typeFilter as never
    ).subscribe({
      next: page => {
        this.leaves.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Impossible de charger les demandes de congés', 'Fermer', { duration: 4000 });
      }
    });
  }

  protected days(leave: LeaveDto): number {
    const start = new Date(leave.startDate + 'T00:00:00');
    const end = new Date(leave.endDate + 'T00:00:00');
    return Math.round((end.getTime() - start.getTime()) / 86400000) + 1;
  }

  protected openCreate(): void {
    const ref = this.dialog.open(LeaveDialogComponent, { width: '480px', disableClose: true });
    ref.afterClosed().subscribe(saved => {
      if (saved) {
        this.load();
      }
    });
  }

  protected approve(leave: LeaveDto): void {
    this.review(leave, 'APPROVED');
  }

  protected reject(leave: LeaveDto): void {
    const comment = prompt('Motif du rejet (optionnel) :');
    if (comment === null) {
      return;
    }
    this.review(leave, 'REJECTED', comment.trim() || undefined);
  }

  protected cancel(leave: LeaveDto): void {
    if (!confirm(`Annuler la demande de congés du ${leave.startDate} au ${leave.endDate} ?`)) {
      return;
    }
    this.leavesService.cancel(leave.id).subscribe({
      next: () => this.load(),
      error: err => {
        const message = (err as { error?: { message?: string } })?.error?.message ?? 'Annulation impossible';
        this.snackBar.open(message, 'Fermer', { duration: 5000 });
      }
    });
  }

  private review(leave: LeaveDto, status: 'APPROVED' | 'REJECTED', comment?: string): void {
    this.leavesService.review(leave.id, { status, comment }).subscribe({
      next: () => this.load(),
      error: err => {
        const message = (err as { error?: { message?: string } })?.error?.message ?? 'Décision impossible';
        this.snackBar.open(message, 'Fermer', { duration: 5000 });
      }
    });
  }
}
