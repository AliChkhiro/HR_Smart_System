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
import { EmployeesService } from '../../core/services/employees.service';
import { DepartmentsService } from '../../core/services/departments.service';
import { AuthService } from '../../core/services/auth.service';
import { EmployeeDto } from '../../core/models/employee.model';
import { DepartmentDto } from '../../core/models/department.model';
import { EmployeeDialogComponent, EmployeeDialogData } from './employee-dialog';
import { EmployeeSkillsDialogComponent, EmployeeSkillsDialogData } from './employee-skills-dialog';

@Component({
  selector: 'app-employees',
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
  templateUrl: './employees.html',
  styleUrl: './employees.scss',
})
export class Employees {
  private readonly employeesService = inject(EmployeesService);
  private readonly departmentsService = inject(DepartmentsService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly isAdmin = inject(AuthService).isAdmin;
  protected readonly displayedColumns = ['name', 'jobTitle', 'department', 'hireDate', 'status', 'skills', 'actions'];

  protected readonly employees = signal<EmployeeDto[]>([]);
  protected readonly departments = signal<DepartmentDto[]>([]);
  protected readonly loading = signal(false);
  protected search = '';
  protected departmentFilter = '';

  constructor() {
    this.load();
    this.departmentsService.list().subscribe({
      next: page => this.departments.set(page.content),
      error: () => this.departments.set([])
    });
  }

  protected initials(employee: EmployeeDto): string {
    return (employee.firstName.charAt(0) + employee.lastName.charAt(0)).toUpperCase();
  }

  protected load(): void {    this.loading.set(true);
    const departmentId = this.departmentFilter ? Number(this.departmentFilter) : undefined;
    this.employeesService.list(this.search.trim() || undefined, departmentId).subscribe({
      next: page => {
        this.employees.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Impossible de charger les employés', 'Fermer', { duration: 4000 });
      }
    });
  }

  protected openCreate(): void {
    this.openDialog(null);
  }

  protected openEdit(employee: EmployeeDto): void {
    this.openDialog(employee);
  }

  protected openSkills(employee: EmployeeDto): void {
    const data: EmployeeSkillsDialogData = { employee };
    const ref = this.dialog.open(EmployeeSkillsDialogComponent, { data, width: '520px' });
    ref.afterClosed().subscribe(updated => {
      if (updated) {
        this.load();
      }
    });
  }

  protected delete(employee: EmployeeDto): void {
    if (!confirm(`Supprimer le dossier de ${employee.firstName} ${employee.lastName} ?`)) {
      return;
    }
    this.employeesService.delete(employee.id).subscribe({
      next: () => {
        this.employees.update(list => list.filter(e => e.id !== employee.id));
        this.snackBar.open('Employé supprimé', 'Fermer', { duration: 3000 });
      },
      error: err => {
        const message = (err as { error?: { message?: string } })?.error?.message ?? 'Suppression impossible';
        this.snackBar.open(message, 'Fermer', { duration: 5000 });
      }
    });
  }

  private openDialog(employee: EmployeeDto | null): void {
    const data: EmployeeDialogData = { employee, departments: this.departments() };
    const ref = this.dialog.open(EmployeeDialogComponent, { data, width: '520px', disableClose: true });
    ref.afterClosed().subscribe(saved => {
      if (saved) {
        this.load();
      }
    });
  }
}
