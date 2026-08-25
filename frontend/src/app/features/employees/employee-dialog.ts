import { Component, Inject, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EmployeesService } from '../../core/services/employees.service';
import { UsersService } from '../../core/services/users.service';
import { EmployeeDto } from '../../core/models/employee.model';
import { DepartmentDto } from '../../core/models/department.model';
import { UserDto } from '../../core/models/user.model';

export interface EmployeeDialogData {
  employee: EmployeeDto | null;
  departments: DepartmentDto[];
}

@Component({
  selector: 'app-employee-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './employee-dialog.html',
  styleUrl: './employee-dialog.scss',
})
export class EmployeeDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly employeesService = inject(EmployeesService);
  private readonly usersService = inject(UsersService);
  private readonly dialogRef = inject(MatDialogRef<EmployeeDialogComponent>);
  private readonly snackBar = inject(MatSnackBar);
  private readonly data = inject<EmployeeDialogData>(MAT_DIALOG_DATA);

  protected readonly isEdit = this.data.employee !== null;
  protected readonly departments = this.data.departments;
  protected readonly users = signal<UserDto[]>([]);
  protected loadingUsers = true;
  protected submitting = false;

  protected readonly form = this.fb.group({
    userId: [this.data.employee?.userId ?? null, this.isEdit ? [] : [Validators.required]],
    jobTitle: [this.data.employee?.jobTitle ?? '', [Validators.required]],
    departmentId: [this.data.employee?.departmentId ?? null],
    hireDate: [this.data.employee?.hireDate ?? '', [Validators.required]],
    status: [this.data.employee?.status ?? 'ACTIVE']
  });

  constructor() {
    this.usersService.list(undefined, undefined, 0, 100, !this.isEdit).subscribe({
      next: page => {
        this.users.set(page.content);
        this.loadingUsers = false;
      },
      error: () => {
        this.users.set([]);
        this.loadingUsers = false;
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting = true;
    const { userId, jobTitle, departmentId, hireDate, status } = this.form.getRawValue();
    if (this.isEdit && this.data.employee) {
      this.employeesService.update(this.data.employee.id, {
        jobTitle: jobTitle || undefined,
        departmentId: departmentId ?? undefined,
        hireDate: hireDate || undefined,
        status: status as 'ACTIVE' | 'INACTIVE'
      }).subscribe({
        next: () => {
          this.snackBar.open('Employé mis à jour', 'Fermer', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: err => this.handleError(err)
      });
    } else {
      this.employeesService.create({
        userId: userId!,
        jobTitle: jobTitle!,
        departmentId: departmentId ?? undefined,
        hireDate: hireDate!
      }).subscribe({
        next: () => {
          this.snackBar.open('Employé créé', 'Fermer', { duration: 3000 });
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
