import { Component, Inject, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DepartmentsService } from '../../core/services/departments.service';
import { UsersService } from '../../core/services/users.service';
import { DepartmentDto } from '../../core/models/department.model';
import { UserDto } from '../../core/models/user.model';

export interface DepartmentDialogData {
  department: DepartmentDto | null;
}

@Component({
  selector: 'app-department-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './department-dialog.html',
  styleUrl: './department-dialog.scss',
})
export class DepartmentDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly departmentsService = inject(DepartmentsService);
  private readonly usersService = inject(UsersService);
  private readonly dialogRef = inject(MatDialogRef<DepartmentDialogComponent>);
  private readonly snackBar = inject(MatSnackBar);
  private readonly data = inject<DepartmentDialogData>(MAT_DIALOG_DATA);

  protected readonly isEdit = this.data.department !== null;
  protected submitting = false;
  protected readonly managers = signal<UserDto[]>([]);

  protected readonly form = this.fb.group({
    name: [this.data.department?.name ?? '', [Validators.required]],
    description: [this.data.department?.description ?? ''],
    managerId: [this.data.department?.managerId ?? null]
  });

  constructor() {
    this.usersService.list(undefined, undefined, 0, 100).subscribe({
      next: page => this.managers.set(page.content),
      error: () => this.managers.set([])
    });
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting = true;
    const { name, description, managerId } = this.form.getRawValue();
    const request = {
      name: name!,
      description: description || undefined,
      managerId: managerId ?? undefined
    };
    const action = this.isEdit && this.data.department
      ? this.departmentsService.update(this.data.department.id, request)
      : this.departmentsService.create(request);
    action.subscribe({
      next: () => {
        this.snackBar.open(this.isEdit ? 'Département mis à jour' : 'Département créé', 'Fermer', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: err => this.handleError(err)
    });
  }

  private handleError(err: unknown): void {
    this.submitting = false;
    const message =
      (err as { error?: { message?: string } })?.error?.message ??
      'Une erreur est survenue. Vérifiez vos informations.';
    this.snackBar.open(message, 'Fermer', { duration: 5000 });
  }
}
