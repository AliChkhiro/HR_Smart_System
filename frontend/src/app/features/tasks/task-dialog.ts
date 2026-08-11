import { Component, Inject, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TasksService } from '../../core/services/tasks.service';
import { EmployeesService } from '../../core/services/employees.service';
import { TaskDto, TaskPriority, TaskStatus } from '../../core/models/task.model';
import { ProjectDto } from '../../core/models/project.model';
import { EmployeeDto } from '../../core/models/employee.model';

export interface TaskDialogData {
  task: TaskDto | null;
  projects: ProjectDto[];
}

@Component({
  selector: 'app-task-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './task-dialog.html',
  styleUrl: './task-dialog.scss',
})
export class TaskDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly tasksService = inject(TasksService);
  private readonly employeesService = inject(EmployeesService);
  private readonly dialogRef = inject(MatDialogRef<TaskDialogComponent>);
  private readonly snackBar = inject(MatSnackBar);
  private readonly data = inject<TaskDialogData>(MAT_DIALOG_DATA);

  protected readonly isEdit = this.data.task !== null;
  protected readonly projects = this.data.projects;
  protected readonly statuses: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'BLOCKED', 'DONE'];
  protected readonly priorities: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
  protected readonly employees = signal<EmployeeDto[]>([]);
  protected submitting = false;

  protected readonly form = this.fb.group({
    name: [this.data.task?.name ?? '', [Validators.required]],
    description: [this.data.task?.description ?? ''],
    projectId: [this.data.task?.projectId ?? null, [Validators.required]],
    assigneeId: [this.data.task?.assigneeId ?? null],
    status: [this.data.task?.status ?? 'TODO'],
    priority: [this.data.task?.priority ?? 'MEDIUM'],
    estimatedHours: [this.data.task?.estimatedHours ?? null],
    startDate: [this.data.task?.startDate ?? ''],
    dueDate: [this.data.task?.dueDate ?? '']
  });

  constructor() {
    this.employeesService.list().subscribe({
      next: page => this.employees.set(page.content),
      error: () => this.employees.set([])
    });
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting = true;
    const { name, description, projectId, assigneeId, status, priority, estimatedHours, startDate, dueDate } =
      this.form.getRawValue();
    if (this.isEdit && this.data.task) {
      this.tasksService.update(this.data.task.id, {
        name: name || undefined,
        description: description || undefined,
        projectId: projectId ?? undefined,
        assigneeId: assigneeId ?? undefined,
        status: status as TaskStatus,
        priority: priority as TaskPriority,
        estimatedHours: estimatedHours ?? undefined,
        startDate: startDate || undefined,
        dueDate: dueDate || undefined
      }).subscribe({
        next: () => {
          this.snackBar.open('Tâche mise à jour', 'Fermer', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: err => this.handleError(err)
      });
    } else {
      this.tasksService.create({
        name: name!,
        description: description || undefined,
        projectId: projectId!,
        assigneeId: assigneeId ?? undefined,
        status: status as TaskStatus,
        priority: priority as TaskPriority,
        estimatedHours: estimatedHours ?? undefined,
        startDate: startDate || undefined,
        dueDate: dueDate || undefined
      }).subscribe({
        next: () => {
          this.snackBar.open('Tâche créée', 'Fermer', { duration: 3000 });
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
