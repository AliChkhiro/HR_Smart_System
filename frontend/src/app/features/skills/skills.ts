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
import { SkillsService } from '../../core/services/skills.service';
import { AuthService } from '../../core/services/auth.service';
import { SkillDto } from '../../core/models/skill.model';
import { SkillDialogComponent, SkillDialogData } from './skill-dialog';

@Component({
  selector: 'app-skills',
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
  templateUrl: './skills.html',
  styleUrl: './skills.scss',
})
export class Skills {
  private readonly skillsService = inject(SkillsService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly isAdmin = inject(AuthService).isAdmin;
  protected readonly displayedColumns = ['name', 'category', 'employeeCount', 'actions'];

  protected readonly skills = signal<SkillDto[]>([]);
  protected readonly loading = signal(false);
  protected search = '';

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.skillsService.list(this.search.trim() || undefined).subscribe({
      next: page => {
        this.skills.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Impossible de charger les compétences', 'Fermer', { duration: 4000 });
      }
    });
  }

  protected openCreate(): void {
    this.openDialog(null);
  }

  protected openEdit(skill: SkillDto): void {
    this.openDialog(skill);
  }

  protected delete(skill: SkillDto): void {
    if (!confirm(`Supprimer la compétence « ${skill.name} » ?`)) {
      return;
    }
    this.skillsService.delete(skill.id).subscribe({
      next: () => {
        this.skills.update(list => list.filter(s => s.id !== skill.id));
        this.snackBar.open('Compétence supprimée', 'Fermer', { duration: 3000 });
      },
      error: err => {
        const message = (err as { error?: { message?: string } })?.error?.message ?? 'Suppression impossible';
        this.snackBar.open(message, 'Fermer', { duration: 5000 });
      }
    });
  }

  private openDialog(skill: SkillDto | null): void {
    const data: SkillDialogData = { skill };
    const ref = this.dialog.open(SkillDialogComponent, { data, width: '480px', disableClose: true });
    ref.afterClosed().subscribe(saved => {
      if (saved) {
        this.load();
      }
    });
  }
}
