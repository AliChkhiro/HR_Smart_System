import { Component, Inject, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ProjectsService } from '../../core/services/projects.service';
import { ProjectDto, ProjectPriority, ProjectStatus } from '../../core/models/project.model';

export interface ProjectDialogData {
  project: ProjectDto | null;
}

@Component({
  selector: 'app-project-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './project-dialog.html',
  styleUrl: './project-dialog.scss',
})
export class ProjectDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly projectsService = inject(ProjectsService);
  private readonly dialogRef = inject(MatDialogRef<ProjectDialogComponent>);
  private readonly snackBar = inject(MatSnackBar);
  private readonly data = inject<ProjectDialogData>(MAT_DIALOG_DATA);

  protected readonly isEdit = this.data.project !== null;
  protected readonly statuses: ProjectStatus[] = ['PLANNED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CANCELLED'];
  protected readonly priorities: ProjectPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
  protected submitting = false;

  protected readonly form = this.fb.group({
    name: [this.data.project?.name ?? '', [Validators.required]],
    description: [this.data.project?.description ?? ''],
    startDate: [this.data.project?.startDate ?? ''],
    endDate: [this.data.project?.endDate ?? ''],
    status: [this.data.project?.status ?? 'PLANNED'],
    priority: [this.data.project?.priority ?? 'MEDIUM']
  });

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting = true;
    const { name, description, startDate, endDate, status, priority } = this.form.getRawValue();
    if (this.isEdit && this.data.project) {
      this.projectsService.update(this.data.project.id, {
        name: name || undefined,
        description: description || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        status: status as ProjectStatus,
        priority: priority as ProjectPriority
      }).subscribe({
        next: () => {
          this.snackBar.open('Projet mis à jour', 'Fermer', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: err => this.handleError(err)
      });
    } else {
      this.projectsService.create({
        name: name!,
        description: description || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        status: status as ProjectStatus,
        priority: priority as ProjectPriority
      }).subscribe({
        next: () => {
          this.snackBar.open('Projet créé', 'Fermer', { duration: 3000 });
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
