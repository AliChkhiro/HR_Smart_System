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
import { ProjectsService } from '../../core/services/projects.service';
import { AuthService } from '../../core/services/auth.service';
import { ProjectDto, ProjectPriority, ProjectStatus } from '../../core/models/project.model';
import { ProjectDialogComponent, ProjectDialogData } from './project-dialog';
import { ProjectMembersDialogComponent, ProjectMembersDialogData } from './project-members-dialog';

@Component({
  selector: 'app-projects',
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
  templateUrl: './projects.html',
  styleUrl: './projects.scss',
})
export class Projects {
  private readonly projectsService = inject(ProjectsService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly isAdmin = inject(AuthService).isAdmin;
  protected readonly displayedColumns = ['name', 'dates', 'status', 'priority', 'memberCount', 'actions'];

  protected readonly statuses: ProjectStatus[] = ['PLANNED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CANCELLED'];
  protected readonly priorities: ProjectPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
  protected readonly statusLabels: Record<string, string> = {
    PLANNED: 'Planifié',
    IN_PROGRESS: 'En cours',
    ON_HOLD: 'En pause',
    COMPLETED: 'Terminé',
    CANCELLED: 'Annulé'
  };
  protected readonly priorityLabels: Record<string, string> = {
    LOW: 'Basse',
    MEDIUM: 'Moyenne',
    HIGH: 'Haute',
    URGENT: 'Urgente'
  };

  protected readonly projects = signal<ProjectDto[]>([]);
  protected readonly loading = signal(false);
  protected search = '';
  protected statusFilter = '';

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.projectsService.list(this.search.trim() || undefined, this.statusFilter || undefined).subscribe({
      next: page => {
        this.projects.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Impossible de charger les projets', 'Fermer', { duration: 4000 });
      }
    });
  }

  protected openCreate(): void {
    this.openDialog(null);
  }

  protected openEdit(project: ProjectDto): void {
    this.openDialog(project);
  }

  protected openMembers(project: ProjectDto): void {
    const data: ProjectMembersDialogData = { project };
    const ref = this.dialog.open(ProjectMembersDialogComponent, { data, width: '560px' });
    ref.afterClosed().subscribe(() => this.load());
  }

  protected delete(project: ProjectDto): void {
    if (!confirm(`Supprimer le projet « ${project.name} » ?`)) {
      return;
    }
    this.projectsService.delete(project.id).subscribe({
      next: () => {
        this.projects.update(list => list.filter(p => p.id !== project.id));
        this.snackBar.open('Projet supprimé', 'Fermer', { duration: 3000 });
      },
      error: err => {
        const message = (err as { error?: { message?: string } })?.error?.message ?? 'Suppression impossible';
        this.snackBar.open(message, 'Fermer', { duration: 5000 });
      }
    });
  }

  private openDialog(project: ProjectDto | null): void {
    const data: ProjectDialogData = { project };
    const ref = this.dialog.open(ProjectDialogComponent, { data, width: '520px', disableClose: true });
    ref.afterClosed().subscribe(saved => {
      if (saved) {
        this.load();
      }
    });
  }
}
