import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { EmployeesService } from '../../core/services/employees.service';
import { LeavesService } from '../../core/services/leaves.service';
import { NotificationsService } from '../../core/services/notifications.service';
import { UsersService } from '../../core/services/users.service';
import { EmployeeDto } from '../../core/models/employee.model';
import { LeaveDto } from '../../core/models/leave.model';
import { NotificationDto } from '../../core/models/notification.model';
import { UserRole } from '../../core/models/user.model';

@Component({
  selector: 'app-profile',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatDividerModule,
    MatListModule
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile {
  private readonly authService = inject(AuthService);
  private readonly employeesService = inject(EmployeesService);
  private readonly leavesService = inject(LeavesService);
  private readonly notificationsService = inject(NotificationsService);
  private readonly usersService = inject(UsersService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly user = this.authService.user;
  protected readonly employee = signal<EmployeeDto | null>(null);
  protected readonly leaves = signal<LeaveDto[]>([]);
  protected readonly recentActivities = signal<NotificationDto[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly saving = signal(false);
  protected readonly selectedTab = signal(0);
  protected readonly editing = signal(false);

  protected readonly form = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required]
  });

  protected readonly roleLabels: Record<string, string> = {
    ADMIN: 'Administrateur',
    RH: 'Responsable RH',
    CHEF_PROJET: 'Chef de projet',
    EMPLOYEE: 'Employé'
  };

  protected readonly leaveTypeLabels: Record<string, string> = {
    ANNUAL: 'Annuel',
    SICK: 'Maladie',
    MATERNITY: 'Maternité',
    PATERNITY: 'Paternité',
    UNPAID: 'Sans solde',
    OTHER: 'Autre'
  };

  protected readonly leaveStatusLabels: Record<string, string> = {
    PENDING: 'En attente',
    APPROVED: 'Approuvé',
    REJECTED: 'Rejeté',
    CANCELLED: 'Annulé'
  };

  protected readonly notificationIcons: Record<string, string> = {
    LEAVE_REQUEST: 'beach_access',
    LEAVE_REVIEW: 'fact_check',
    TASK_ASSIGNED: 'assignment_turned_in'
  };

  constructor() {
    this.load();
  }

  protected initials(user: { firstName: string; lastName: string }): string {
    return (user.firstName.charAt(0) + user.lastName.charAt(0)).toUpperCase();
  }

  protected roleChipClass(role: UserRole): string {
    switch (role) {
      case 'ADMIN':
        return 'chip-violet';
      case 'RH':
        return 'chip-cyan';
      case 'CHEF_PROJET':
        return 'chip-blue';
      default:
        return 'chip-gray';
    }
  }

  protected leaveStatusClass(status: string): string {
    switch (status) {
      case 'APPROVED':
        return 'chip-green';
      case 'REJECTED':
        return 'chip-red';
      case 'CANCELLED':
        return 'chip-gray';
      default:
        return 'chip-amber';
    }
  }

  protected days(leave: LeaveDto): number {
    const start = new Date(leave.startDate + 'T00:00:00');
    const end = new Date(leave.endDate + 'T00:00:00');
    return Math.round((end.getTime() - start.getTime()) / 86400000) + 1;
  }

  protected seniorityYears(): number {
    const employee = this.employee();
    if (!employee?.hireDate) {
      return 0;
    }
    const hire = new Date(employee.hireDate + 'T00:00:00');
    const now = new Date();
    let years = now.getFullYear() - hire.getFullYear();
    const m = now.getMonth() - hire.getMonth();
    if (m < 0 || (m === 0 && now.getDate() < hire.getDate())) {
      years--;
    }
    return Math.max(0, years);
  }

  protected approvedDays(): number {
    return this.leaves().filter(l => l.status === 'APPROVED').reduce((sum, l) => sum + this.days(l), 0);
  }

  protected pendingLeaves(): LeaveDto[] {
    return this.leaves().filter(l => l.status === 'PENDING');
  }

  protected nextLeave(): LeaveDto | null {
    const upcoming = this.leaves()
      .filter(l => l.status === 'APPROVED' && new Date(l.startDate + 'T00:00:00') >= new Date(new Date().toDateString()))
      .sort((a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime());
    return upcoming[0] ?? null;
  }

  protected skillLevelClass(level: number): string {
    if (level >= 4) {
      return 'skill-fill-high';
    }
    if (level >= 3) {
      return 'skill-fill-mid';
    }
    return 'skill-fill-low';
  }

  protected skillLevelPercent(level: number): number {
    return Math.round(level * 20);
  }

  protected reloadProfileFromSession(): void {
    const current = this.user();
    if (!current) {
      return;
    }
    this.form.patchValue({ firstName: current.firstName, lastName: current.lastName });
  }

  protected startEdit(): void {
    const current = this.user();
    if (!current) {
      return;
    }
    this.form.setValue({ firstName: current.firstName, lastName: current.lastName });
    this.editing.set(true);
    this.selectedTab.set(1);
  }

  protected toggleEdit(): void {
    if (this.editing()) {
      this.cancelEdit();
    } else {
      this.startEdit();
    }
  }

  protected save(): void {
    if (this.form.invalid) {
      return;
    }
    const current = this.user();
    if (!current) {
      return;
    }
    this.saving.set(true);
    this.usersService.update(current.id, {
      firstName: this.form.value.firstName ?? undefined,
      lastName: this.form.value.lastName ?? undefined
    }).subscribe({
      next: updated => {
        this.authService.setSession({
          accessToken: this.authService.accessToken!,
          refreshToken: this.authService.refreshToken!,
          user: updated
        });
        this.reloadProfileFromSession();
        this.saving.set(false);
        this.editing.set(false);
        this.snackBar.open('Profil mis à jour avec succès', 'Fermer', { duration: 3500 });
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open('Impossible de mettre à jour le profil', 'Fermer', { duration: 4000 });
      }
    });
  }

  protected cancelEdit(): void {
    this.reloadProfileFromSession();
    this.editing.set(false);
  }

  protected openLeaves(): void {
    this.router.navigate(['/leaves']);
  }

  protected retry(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(false);
    const current = this.user();

    if (current) {
      this.reloadProfileFromSession();
    }

    this.employeesService.me().subscribe({
      next: employee => {
        this.employee.set(employee);
        this.loadLeaves(employee.id);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      }
    });

    this.notificationsService.list(0, 8).subscribe({
      next: page => this.recentActivities.set(page.content)
    });
  }

  private loadLeaves(employeeId: number): void {
    this.leavesService.list(employeeId).subscribe({
      next: page => {
        this.leaves.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      }
    });
  }
}
