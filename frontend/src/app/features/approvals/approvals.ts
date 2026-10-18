import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UsersService } from '../../core/services/users.service';
import { UserDto } from '../../core/models/user.model';

@Component({
  selector: 'app-approvals',
  imports: [
    DatePipe,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './approvals.html',
  styleUrl: './approvals.scss',
})
export class Approvals {
  private readonly usersService = inject(UsersService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly displayedColumns = ['name', 'email', 'createdAt', 'actions'];
  protected readonly pending = signal<UserDto[]>([]);
  protected readonly loading = signal(false);
  protected readonly processingId = signal<number | null>(null);

  constructor() {
    this.load();
  }

  protected initials(user: UserDto): string {
    return (user.firstName.charAt(0) + user.lastName.charAt(0)).toUpperCase();
  }

  protected load(): void {
    this.loading.set(true);
    this.usersService.list(undefined, undefined, 0, 100, undefined, 'PENDING').subscribe({
      next: page => {
        this.pending.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Impossible de charger les demandes', 'Fermer', { duration: 4000 });
      }
    });
  }

  protected approve(user: UserDto): void {
    this.processingId.set(user.id);
    this.usersService.approve(user.id).subscribe({
      next: () => {
        this.processingId.set(null);
        this.remove(user.id);
        this.snackBar.open(`${user.firstName} ${user.lastName} : compte validé`, 'Fermer', { duration: 3000 });
      },
      error: () => {
        this.processingId.set(null);
        this.snackBar.open('Validation impossible', 'Fermer', { duration: 4000 });
      }
    });
  }

  protected reject(user: UserDto): void {
    if (!confirm(`Refuser la demande de ${user.firstName} ${user.lastName} ?`)) {
      return;
    }
    this.processingId.set(user.id);
    this.usersService.reject(user.id).subscribe({
      next: () => {
        this.processingId.set(null);
        this.remove(user.id);
        this.snackBar.open('Demande refusée', 'Fermer', { duration: 3000 });
      },
      error: () => {
        this.processingId.set(null);
        this.snackBar.open('Refus impossible', 'Fermer', { duration: 4000 });
      }
    });
  }

  private remove(id: number): void {
    this.pending.update(list => list.filter(u => u.id !== id));
  }
}
