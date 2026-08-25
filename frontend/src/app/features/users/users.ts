import { Component, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { UsersService } from '../../core/services/users.service';
import { UserDto, UserRole, UserStatus } from '../../core/models/user.model';
import { UserDialogComponent, UserDialogData } from './user-dialog';

@Component({
  selector: 'app-users',
  imports: [
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    FormsModule
  ],
  templateUrl: './users.html',
  styleUrl: './users.scss',
})
export class Users {
  private readonly usersService = inject(UsersService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly displayedColumns = ['name', 'email', 'role', 'status', 'actions'];
  protected readonly roles: UserRole[] = ['ADMIN', 'RH', 'CHEF_PROJET', 'EMPLOYEE'];
  protected readonly roleLabels: Record<string, string> = {
    ADMIN: 'Administrateur',
    RH: 'RH',
    CHEF_PROJET: 'Chef de projet',
    EMPLOYEE: 'Employé'
  };

  protected readonly users = signal<UserDto[]>([]);
  protected readonly loading = signal(false);
  protected search = '';

  protected initials(user: UserDto): string {
    return (user.firstName.charAt(0) + user.lastName.charAt(0)).toUpperCase();
  }

  protected roleChipClass(role: UserRole): string {
    switch (role) {
      case 'ADMIN':
        return 'chip-violet';
      case 'RH':
        return 'chip-cyan';
      case 'CHEF_PROJET':
        return 'chip-blue';
      default:
        return 'chip-gray';
    }
  }

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.usersService.list(this.search.trim() || undefined).subscribe({
      next: page => {
        this.users.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Impossible de charger les utilisateurs', 'Fermer', { duration: 4000 });
      }
    });
  }

  protected openCreate(): void {
    this.openDialog(null);
  }

  protected openEdit(user: UserDto): void {
    this.openDialog(user);
  }

  protected toggleStatus(user: UserDto): void {
    const nextStatus: UserStatus = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
    this.usersService.update(user.id, { status: nextStatus }).subscribe({
      next: updated => {
        this.users.update(list => list.map(u => (u.id === updated.id ? updated : u)));
        this.snackBar.open(nextStatus === 'ACTIVE' ? 'Compte réactivé' : 'Compte désactivé', 'Fermer', { duration: 3000 });
      },
      error: () => this.snackBar.open('Action impossible', 'Fermer', { duration: 4000 })
    });
  }

  protected delete(user: UserDto): void {
    if (!confirm(`Supprimer définitivement l'utilisateur ${user.firstName} ${user.lastName} ?`)) {
      return;
    }
    this.usersService.delete(user.id).subscribe({
      next: () => {
        this.users.update(list => list.filter(u => u.id !== user.id));
        this.snackBar.open('Utilisateur supprimé', 'Fermer', { duration: 3000 });
      },
      error: () => this.snackBar.open('Suppression impossible', 'Fermer', { duration: 4000 })
    });
  }

  private openDialog(user: UserDto | null): void {
    const data: UserDialogData = { user };
    const ref = this.dialog.open(UserDialogComponent, { data, width: '480px', disableClose: true });
    ref.afterClosed().subscribe(saved => {
      if (saved) {
        this.load();
      }
    });
  }
}
