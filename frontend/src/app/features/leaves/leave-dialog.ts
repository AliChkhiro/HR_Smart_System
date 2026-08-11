import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LeavesService } from '../../core/services/leaves.service';
import { LeaveDto, LeaveType } from '../../core/models/leave.model';

export interface LeaveDialogData {
  leave: LeaveDto | null;
}

@Component({
  selector: 'app-leave-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './leave-dialog.html',
  styleUrl: './leave-dialog.scss',
})
export class LeaveDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly leavesService = inject(LeavesService);
  private readonly dialogRef = inject(MatDialogRef<LeaveDialogComponent>);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly isEdit = false;
  protected readonly types: LeaveType[] = ['ANNUAL', 'SICK', 'MATERNITY', 'PATERNITY', 'UNPAID', 'OTHER'];
  protected submitting = false;

  protected readonly form = this.fb.group({
    type: ['ANNUAL', [Validators.required]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    reason: ['']
  });

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting = true;
    const { type, startDate, endDate, reason } = this.form.getRawValue();
    this.leavesService.create({
      type: type as LeaveType,
      startDate: startDate!,
      endDate: endDate!,
      reason: reason || undefined
    }).subscribe({
      next: () => {
        this.snackBar.open('Demande de congés envoyée', 'Fermer', { duration: 3000 });
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
