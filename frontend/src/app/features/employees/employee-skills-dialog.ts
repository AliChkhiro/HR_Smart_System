import { Component, Inject, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EmployeesService } from '../../core/services/employees.service';
import { SkillsService } from '../../core/services/skills.service';
import { EmployeeDto } from '../../core/models/employee.model';
import { SkillDto } from '../../core/models/skill.model';

export interface EmployeeSkillsDialogData {
  employee: EmployeeDto;
}

@Component({
  selector: 'app-employee-skills-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './employee-skills-dialog.html',
  styleUrl: './employee-skills-dialog.scss',
})
export class EmployeeSkillsDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly employeesService = inject(EmployeesService);
  private readonly skillsService = inject(SkillsService);
  private readonly dialogRef = inject(MatDialogRef<EmployeeSkillsDialogComponent>);
  private readonly snackBar = inject(MatSnackBar);
  private readonly data = inject<EmployeeSkillsDialogData>(MAT_DIALOG_DATA);

  protected readonly employee = this.data.employee;
  protected readonly skills = signal<SkillDto[]>([]);
  protected readonly currentSkills = signal<EmployeeDto['skills']>(this.data.employee.skills);
  protected readonly availableSkills = signal<SkillDto[]>([]);
  protected submitting = false;

  protected readonly form = this.fb.group({
    skillId: [null as number | null, [Validators.required]],
    level: [3, [Validators.required, Validators.min(1), Validators.max(5)]]
  });

  constructor() {
    this.skillsService.list().subscribe({
      next: page => {
        this.skills.set(page.content);
        this.refreshAvailable();
      },
      error: () => this.snackBar.open('Impossible de charger les compétences', 'Fermer', { duration: 4000 })
    });
  }

  protected addSkill(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting = true;
    const { skillId, level } = this.form.getRawValue();
    this.employeesService.addSkill(this.employee.id, { skillId: skillId!, level: level! }).subscribe({
      next: employee => {
        this.currentSkills.set(employee.skills);
        this.form.reset({ skillId: null, level: 3 });
        this.refreshAvailable();
        this.submitting = false;
      },
      error: err => {
        this.submitting = false;
        this.handleError(err);
      }
    });
  }

  protected updateLevel(skillId: number, level: number): void {
    this.employeesService.updateSkill(this.employee.id, skillId, { skillId, level }).subscribe({
      next: employee => this.currentSkills.set(employee.skills),
      error: err => this.handleError(err)
    });
  }

  protected removeSkill(skillId: number): void {
    this.employeesService.removeSkill(this.employee.id, skillId).subscribe({
      next: employee => {
        this.currentSkills.set(employee.skills);
        this.refreshAvailable();
      },
      error: err => this.handleError(err)
    });
  }

  protected close(): void {
    this.dialogRef.close(true);
  }

  private refreshAvailable(): void {
    const assigned = new Set(this.currentSkills().map(s => s.skillId));
    this.availableSkills.set(this.skills().filter(s => !assigned.has(s.id)));
  }

  private handleError(err: unknown): void {
    const message =
      (err as { error?: { message?: string } })?.error?.message ??
      'Une erreur est survenue';
    this.snackBar.open(message, 'Fermer', { duration: 5000 });
  }
}
