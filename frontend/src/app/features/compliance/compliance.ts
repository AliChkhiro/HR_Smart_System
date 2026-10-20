import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RgpdService } from '../../core/services/rgpd.service';
import { AuditLogDto, ErasureRequestDto, ErasureStatus } from '../../core/models/rgpd.model';

@Component({
  selector: 'app-compliance',
  imports: [
    DatePipe,
    FormsModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTabsModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatSelectModule,
    MatPaginatorModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './compliance.html',
  styleUrl: './compliance.scss',
})
export class Compliance {
  private readonly rgpdService = inject(RgpdService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly auditColumns = ['createdAt', 'action', 'actor', 'entity', 'detail'];
  protected readonly erasureColumns = ['requestedAt', 'user', 'reason', 'status', 'actions'];

  protected readonly audit = signal<AuditLogDto[]>([]);
  protected readonly auditLoading = signal(false);
  protected readonly auditTotal = signal(0);
  protected readonly pageSize = signal(25);
  protected readonly pageIndex = signal(0);

  protected readonly erasures = signal<ErasureRequestDto[]>([]);
  protected readonly erasuresLoading = signal(false);
  protected readonly erasureFilter = signal<ErasureStatus | ''>('PENDING');
  protected readonly decisionInFlight = signal<number | null>(null);

  protected readonly erasureStatusLabels: Record<string, string> = {
    PENDING: 'En attente',
    APPROVED: 'Validée',
    REJECTED: 'Refusée'
  };

  constructor() {
    this.loadAudit(0);
    this.loadErasures();
  }

  protected loadAudit(pageIndex: number): void {
    this.pageIndex.set(pageIndex);
    this.auditLoading.set(true);
    this.rgpdService.auditLog(pageIndex, this.pageSize()).subscribe({
      next: page => {
        this.audit.set(page.content);
        this.auditTotal.set(page.totalElements);
        this.auditLoading.set(false);
      },
      error: () => {
        this.auditLoading.set(false);
        this.snackBar.open("Impossible de charger le journal d'audit", 'Fermer', { duration: 4000 });
      }
    });
  }

  protected onAuditPage(event: PageEvent): void {
    this.pageSize.set(event.pageSize);
    this.loadAudit(event.pageIndex);
  }

  protected loadErasures(): void {
    this.erasuresLoading.set(true);
    const status = this.erasureFilter() || undefined;
    this.rgpdService.listErasureRequests(status).subscribe({
      next: page => {
        this.erasures.set(page.content);
        this.erasuresLoading.set(false);
      },
      error: () => {
        this.erasuresLoading.set(false);
        this.snackBar.open('Impossible de charger les demandes d\u2019effacement', 'Fermer', { duration: 4000 });
      }
    });
  }

  protected approve(request: ErasureRequestDto): void {
    if (!confirm(`Valider l'effacement du compte ${request.userEmail ?? request.userId} ? Le compte sera désactivé.`)) {
      return;
    }
    this.decisionInFlight.set(request.id);
    this.rgpdService.approveErasure(request.id).subscribe({
      next: updated => {
        this.decisionInFlight.set(null);
        this.erasures.update(list => list.map(item => (item.id === updated.id ? updated : item)));
        this.snackBar.open('Effacement validé', 'Fermer', { duration: 3500 });
      },
      error: () => {
        this.decisionInFlight.set(null);
        this.snackBar.open('Validation impossible', 'Fermer', { duration: 4000 });
      }
    });
  }

  protected reject(request: ErasureRequestDto): void {
    const comment = prompt(`Motif du refus pour ${request.userEmail ?? request.userId} (facultatif) :`);
    if (comment === null) {
      return;
    }
    this.decisionInFlight.set(request.id);
    this.rgpdService.rejectErasure(request.id, comment).subscribe({
      next: updated => {
        this.decisionInFlight.set(null);
        this.erasures.update(list => list.map(item => (item.id === updated.id ? updated : item)));
        this.snackBar.open('Demande refusée', 'Fermer', { duration: 3500 });
      },
      error: () => {
        this.decisionInFlight.set(null);
        this.snackBar.open('Refus impossible', 'Fermer', { duration: 4000 });
      }
    });
  }

  protected erasureStatusClass(status: ErasureStatus): string {
    switch (status) {
      case 'APPROVED':
        return 'chip-green';
      case 'REJECTED':
        return 'chip-red';
      default:
        return 'chip-amber';
    }
  }

  protected actionClass(action: string): string {
    if (action.startsWith('RGPD_')) {
      return 'chip-violet';
    }
    if (action === 'LOGIN_SUCCESS' || action === 'USER_APPROVED' || action === 'LEAVE_APPROVED') {
      return 'chip-green';
    }
    if (action === 'LOGIN_FAILURE' || action === 'USER_REJECTED' || action === 'LEAVE_REJECTED'
        || action.endsWith('DELETED')) {
      return 'chip-red';
    }
    if (action === 'ENTITY_CREATED') {
      return 'chip-blue';
    }
    return 'chip-gray';
  }
}
