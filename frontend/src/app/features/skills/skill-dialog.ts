import { Component, Inject, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SkillsService } from '../../core/services/skills.service';
import { SkillDto } from '../../core/models/skill.model';

export interface SkillDialogData {
  skill: SkillDto | null;
}

@Component({
  selector: 'app-skill-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './skill-dialog.html',
  styleUrl: './skill-dialog.scss',
})
export class SkillDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly skillsService = inject(SkillsService);
  private readonly dialogRef = inject(MatDialogRef<SkillDialogComponent>);
  private readonly snackBar = inject(MatSnackBar);
  private readonly data = inject<SkillDialogData>(MAT_DIALOG_DATA);

  protected readonly isEdit = this.data.skill !== null;
  protected readonly categories = ['DEVELOPPEMENT', 'BASE_DE_DONNEES', 'DEVOPS', 'GESTION', 'RH', 'COMMERCIAL', 'GENERAL'];
  protected submitting = false;

  protected readonly form = this.fb.group({
    name: [this.data.skill?.name ?? '', [Validators.required]],
    category: [this.data.skill?.category ?? 'GENERAL', [Validators.required]]
  });

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting = true;
    const { name, category } = this.form.getRawValue();
    const request = { name: name!, category: category! };
    const action = this.isEdit && this.data.skill
      ? this.skillsService.update(this.data.skill.id, request)
      : this.skillsService.create(request);
    action.subscribe({
      next: () => {
        this.snackBar.open(this.isEdit ? 'Compétence mise à jour' : 'Compétence créée', 'Fermer', { duration: 3000 });
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
