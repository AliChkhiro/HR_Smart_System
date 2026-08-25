import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { CdkDragDrop, CdkDropList, CdkDrag, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { MatCardModule } from '@angular/material/card';
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
import { TasksService } from '../../core/services/tasks.service';
import { ProjectsService } from '../../core/services/projects.service';
import { AuthService } from '../../core/services/auth.service';
import { TaskDto, TaskStatus } from '../../core/models/task.model';
import { ProjectDto } from '../../core/models/project.model';
import { TaskDialogComponent, TaskDialogData } from './task-dialog';

interface KanbanColumn {
  status: TaskStatus;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-tasks',
  imports: [
    DatePipe,
    CdkDropList,
    CdkDrag,
    MatCardModule,
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
  templateUrl: './tasks.html',
  styleUrl: './tasks.scss',
})
export class Tasks {
  private readonly tasksService = inject(TasksService);
  private readonly projectsService = inject(ProjectsService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly isAdmin = inject(AuthService).isAdmin;
  protected readonly columns: KanbanColumn[] = [
    { status: 'TODO', label: 'À faire', icon: 'radio_button_unchecked' },
    { status: 'IN_PROGRESS', label: 'En cours', icon: 'hourglass_top' },
    { status: 'BLOCKED', label: 'Bloqué', icon: 'block' },
    { status: 'DONE', label: 'Terminé', icon: 'check_circle' }
  ];
  protected readonly priorityLabels: Record<string, string> = {
    LOW: 'Basse',
    MEDIUM: 'Moyenne',
    HIGH: 'Haute',
    URGENT: 'Urgente'
  };
  protected readonly priorityColors: Record<string, 'primary' | 'accent' | 'warn'> = {
    LOW: 'primary',
    MEDIUM: 'accent',
    HIGH: 'accent',
    URGENT: 'warn'
  };

  protected readonly tasksByStatus = signal<Record<string, TaskDto[]>>({
    TODO: [],
    IN_PROGRESS: [],
    BLOCKED: [],
    DONE: []
  });
  protected readonly projects = signal<ProjectDto[]>([]);
  protected readonly loading = signal(false);
  protected projectFilter = '';
  protected search = '';

  protected columnColor(status: TaskStatus): string {
    switch (status) {
      case 'IN_PROGRESS':
        return 'chip-blue';
      case 'BLOCKED':
        return 'chip-red';
      case 'DONE':
        return 'chip-green';
      default:
        return 'chip-amber';
    }
  }

  protected priorityChipClass(priority: string): string {
    switch (priority) {
      case 'URGENT':
        return 'chip-red';
      case 'HIGH':
        return 'chip-amber';
      case 'MEDIUM':
        return 'chip-blue';
      default:
        return 'chip-gray';
    }
  }

  protected initials(name: string): string {
    const parts = name.trim().split(/\s+/);
    const first = parts[0]?.charAt(0) ?? '';
    const last = parts.length > 1 ? parts[parts.length - 1].charAt(0) : '';
    return (first + last).toUpperCase();
  }

  protected isOverdue(task: TaskDto): boolean {
    if (!task.dueDate || task.status === 'DONE') {
      return false;
    }
    return new Date(task.dueDate + 'T00:00:00') < new Date(new Date().toDateString());
  }

  constructor() {
    this.load();
    this.projectsService.list().subscribe({
      next: page => this.projects.set(page.content),
      error: () => this.projects.set([])
    });
  }

  protected load(): void {
    this.loading.set(true);
    const projectId = this.projectFilter ? Number(this.projectFilter) : undefined;
    this.tasksService.list(this.search.trim() || undefined, projectId).subscribe({
      next: page => {
        const grouped: Record<string, TaskDto[]> = { TODO: [], IN_PROGRESS: [], BLOCKED: [], DONE: [] };
        for (const task of page.content) {
          grouped[task.status] ??= [];
          grouped[task.status].push(task);
        }
        this.tasksByStatus.set(grouped);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Impossible de charger les tâches', 'Fermer', { duration: 4000 });
      }
    });
  }

  protected connectedColumnIds(): string[] {
    return this.columns.map(column => column.status);
  }

  protected drop(event: CdkDragDrop<TaskDto[]>): void {    const targetStatus = (event.container.id) as TaskStatus;
    const grouped = this.tasksByStatus();
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      const sourceStatus = event.previousContainer.id as TaskStatus;
      const moved = grouped[sourceStatus][event.previousIndex];
      transferArrayItem(event.previousContainer.data, event.container.data, event.previousIndex, event.currentIndex);
      this.tasksByStatus.set({ ...grouped });
      this.tasksService.updateStatus(moved.id, targetStatus).subscribe({
        error: () => {
          this.snackBar.open('Impossible de déplacer la tâche', 'Fermer', { duration: 4000 });
          this.load();
        }
      });
    }
  }

  protected openCreate(): void {
    this.openDialog(null);
  }

  protected openEdit(task: TaskDto): void {
    this.openDialog(task);
  }

  protected delete(task: TaskDto): void {
    if (!confirm(`Supprimer la tâche « ${task.name} » ?`)) {
      return;
    }
    this.tasksService.delete(task.id).subscribe({
      next: () => this.load(),
      error: err => {
        const message = (err as { error?: { message?: string } })?.error?.message ?? 'Suppression impossible';
        this.snackBar.open(message, 'Fermer', { duration: 5000 });
      }
    });
  }

  private openDialog(task: TaskDto | null): void {
    const data: TaskDialogData = { task, projects: this.projects() };
    const ref = this.dialog.open(TaskDialogComponent, { data, width: '560px', disableClose: true });
    ref.afterClosed().subscribe(saved => {
      if (saved) {
        this.load();
      }
    });
  }
}
