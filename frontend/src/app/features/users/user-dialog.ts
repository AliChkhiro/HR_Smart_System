import { Component, Inject, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UsersService } from '../../core/services/users.service';
import { UserDto, UserRole } from '../../core/models/user.model';

export interface UserDialogData {
  user: UserDto | null;
}

@Component({
  selector: 'app-user-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './user-dialog.html',
  styleUrl: './user-dialog.scss',
})
export class UserDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly usersService = inject(UsersService);
  private readonly dialogRef = inject(MatDialogRef<UserDialogComponent>);
  private readonly snackBar = inject(MatSnackBar);
  private readonly data = inject<UserDialogData>(MAT_DIALOG_DATA);

  protected readonly roles: UserRole[] = ['ADMIN', 'RH', 'CHEF_PROJET', 'EMPLOYEE'];
  protected readonly roleLabels: Record<string, string> = {
    ADMIN: 'Administrateur',
    RH: 'RH',
    CHEF_PROJET: 'Chef de projet',
    EMPLOYEE: 'Employé'
  };
  protected readonly isEdit = this.data.user !== null;
  protected submitting = false;

  protected readonly form = this.fb.group({
    email: [this.data.user?.email ?? '', [Validators.required, Validators.email]],
    firstName: [this.data.user?.firstName ?? '', [Validators.required]],
    lastName: [this.data.user?.lastName ?? '', [Validators.required]],
    password: ['', this.isEdit ? [] : [Validators.required, Validators.minLength(6)]],
    role: [this.data.user?.role ?? 'EMPLOYEE', [Validators.required]]
  });

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting = true;
    const { email, firstName, lastName, password, role } = this.form.getRawValue();
    if (this.isEdit && this.data.user) {
      this.usersService.update(this.data.user.id, {
        firstName: firstName ?? undefined,
        lastName: lastName ?? undefined,
        role: role ?? undefined
      }).subscribe({
        next: () => {
          this.snackBar.open('Utilisateur mis à jour', 'Fermer', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: err => this.handleError(err)
      });
    } else {
      this.usersService.create({ email: email!, firstName: firstName!, lastName: lastName!, password: password!, role: role as UserRole }).subscribe({
        next: () => {
          this.snackBar.open('Utilisateur créé', 'Fermer', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: err => this.handleError(err)
      });
    }
  }

  private handleError(err: unknown): void {
    this.submitting = false;
    const message =
      (err as { error?: { message?: string } })?.error?.message ??
      'Une erreur est survenue. Vérifiez vos informations.';
    this.snackBar.open(message, 'Fermer', { duration: 5000 });
  }
}
