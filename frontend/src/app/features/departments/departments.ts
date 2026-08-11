import { Component, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { DepartmentsService } from '../../core/services/departments.service';
import { AuthService } from '../../core/services/auth.service';
import { DepartmentDto } from '../../core/models/department.model';
import { DepartmentDialogComponent, DepartmentDialogData } from './department-dialog';

@Component({
  selector: 'app-departments',
  imports: [
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    FormsModule
  ],
  templateUrl: './departments.html',
  styleUrl: './departments.scss',
})
export class Departments {
  private readonly departmentsService = inject(DepartmentsService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly isAdmin = inject(AuthService).isAdmin;
  protected readonly displayedColumns = ['name', 'description', 'manager', 'employeeCount', 'actions'];

  protected readonly departments = signal<DepartmentDto[]>([]);
  protected readonly loading = signal(false);
  protected search = '';

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.departmentsService.list(this.search.trim() || undefined).subscribe({
      next: page => {
        this.departments.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Impossible de charger les départements', 'Fermer', { duration: 4000 });
      }
    });
  }

  protected openCreate(): void {
    this.openDialog(null);
  }

  protected openEdit(department: DepartmentDto): void {
    this.openDialog(department);
  }

  protected delete(department: DepartmentDto): void {
    if (!confirm(`Supprimer le département « ${department.name} » ?`)) {
      return;
    }
    this.departmentsService.delete(department.id).subscribe({
      next: () => {
        this.departments.update(list => list.filter(d => d.id !== department.id));
        this.snackBar.open('Département supprimé', 'Fermer', { duration: 3000 });
      },
      error: err => {
        const message = (err as { error?: { message?: string } })?.error?.message ?? 'Suppression impossible';
        this.snackBar.open(message, 'Fermer', { duration: 5000 });
      }
    });
  }

  private openDialog(department: DepartmentDto | null): void {
    const data: DepartmentDialogData = { department };
    const ref = this.dialog.open(DepartmentDialogComponent, { data, width: '480px', disableClose: true });
    ref.afterClosed().subscribe(saved => {
      if (saved) {
        this.load();
      }
    });
  }
}
