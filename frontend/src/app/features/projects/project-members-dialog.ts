import { Component, Inject, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ProjectsService } from '../../core/services/projects.service';
import { EmployeesService } from '../../core/services/employees.service';
import { ProjectDto, ProjectMemberDto, ProjectMemberRole } from '../../core/models/project.model';
import { EmployeeDto } from '../../core/models/employee.model';

export interface ProjectMembersDialogData {
  project: ProjectDto;
}

@Component({
  selector: 'app-project-members-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatListModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './project-members-dialog.html',
  styleUrl: './project-members-dialog.scss',
})
export class ProjectMembersDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly projectsService = inject(ProjectsService);
  private readonly employeesService = inject(EmployeesService);
  private readonly dialogRef = inject(MatDialogRef<ProjectMembersDialogComponent>);
  private readonly snackBar = inject(MatSnackBar);
  private readonly data = inject<ProjectMembersDialogData>(MAT_DIALOG_DATA);

  protected readonly project = this.data.project;
  protected readonly roleLabels: Record<string, string> = {
    MANAGER: 'Responsable',
    MEMBER: 'Membre'
  };
  protected readonly members = signal<ProjectMemberDto[]>([]);
  protected readonly employees = signal<EmployeeDto[]>([]);
  protected readonly availableEmployees = signal<EmployeeDto[]>([]);
  protected submitting = false;

  protected readonly form = this.fb.group({
    employeeId: [null as number | null, [Validators.required]],
    role: ['MEMBER' as ProjectMemberRole, [Validators.required]]
  });

  constructor() {
    this.loadMembers();
    this.employeesService.list().subscribe({
      next: page => {
        this.employees.set(page.content);
        this.refreshAvailable();
      },
      error: () => this.snackBar.open('Impossible de charger les employés', 'Fermer', { duration: 4000 })
    });
  }

  protected addMember(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting = true;
    const { employeeId, role } = this.form.getRawValue();
    this.projectsService.addMember(this.project.id, { employeeId: employeeId!, role: role as ProjectMemberRole })
      .subscribe({
        next: detail => {
          this.members.set(detail.members);
          this.form.reset({ employeeId: null, role: 'MEMBER' });
          this.refreshAvailable();
          this.submitting = false;
        },
        error: err => {
          this.submitting = false;
          this.handleError(err);
        }
      });
  }

  protected updateRole(employeeId: number, role: ProjectMemberRole): void {
    this.projectsService.updateMember(this.project.id, employeeId, { employeeId, role }).subscribe({
      next: detail => this.members.set(detail.members),
      error: err => this.handleError(err)
    });
  }

  protected removeMember(employeeId: number): void {
    this.projectsService.removeMember(this.project.id, employeeId).subscribe({
      next: detail => {
        this.members.set(detail.members);
        this.refreshAvailable();
      },
      error: err => this.handleError(err)
    });
  }

  protected close(): void {
    this.dialogRef.close(true);
  }

  private loadMembers(): void {
    this.projectsService.get(this.project.id).subscribe({
      next: detail => this.members.set(detail.members),
      error: () => this.snackBar.open('Impossible de charger l\'équipe', 'Fermer', { duration: 4000 })
    });
  }

  private refreshAvailable(): void {
    const assigned = new Set(this.members().map(m => m.employeeId));
    this.availableEmployees.set(this.employees().filter(e => !assigned.has(e.id)));
  }

  private handleError(err: unknown): void {
    const message =
      (err as { error?: { message?: string } })?.error?.message ??
      'Une erreur est survenue';
    this.snackBar.open(message, 'Fermer', { duration: 5000 });
  }
}
