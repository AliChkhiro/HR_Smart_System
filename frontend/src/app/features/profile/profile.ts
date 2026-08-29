import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../core/services/auth.service';
import { UsersService } from '../../core/services/users.service';
import { UserRole } from '../../core/models/user.model';

@Component({
  selector: 'app-profile',
  imports: [
    DatePipe,
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly usersService = inject(UsersService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly user = this.authService.user;
  protected readonly saving = signal(false);

  protected readonly form = this.fb.group({
    firstName: [this.user()?.firstName ?? '', [Validators.required]],
    lastName: [this.user()?.lastName ?? '', [Validators.required]]
  });

  protected initials(): string {
    const u = this.user();
    if (!u) {
      return '';
    }
    return (u.firstName.charAt(0) + u.lastName.charAt(0)).toUpperCase();
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

  protected save(): void {
    const current = this.user();
    if (!current || this.form.invalid) {
      return;
    }
    const { firstName, lastName } = this.form.getRawValue();
    this.saving.set(true);
    this.usersService.update(current.id, {
      firstName: firstName ?? undefined,
      lastName: lastName ?? undefined
    }).subscribe({
      next: updated => {
        this.saving.set(false);
        this.authService.setSession({
          accessToken: this.authService.accessToken!,
          refreshToken: this.authService.refreshToken!,
          user: updated
        });
        this.snackBar.open('Profil mis à jour', 'Fermer', { duration: 3000 });
      },
      error: err => {
        this.saving.set(false);
        const message =
          (err as { error?: { message?: string } })?.error?.message ??
          'Impossible de mettre à jour le profil';
        this.snackBar.open(message, 'Fermer', { duration: 5000 });
      }
    });
  }
}
