import { Component, computed, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TasksService } from '../../core/services/tasks.service';
import { HolidaysService } from '../../core/services/holidays.service';
import { TaskDto } from '../../core/models/task.model';
import { HolidayDto } from '../../core/models/holiday.model';

interface CalendarDay {
  date: Date;
  inMonth: boolean;
  isToday: boolean;
  tasks: TaskDto[];
  holidays: HolidayDto[];
}

@Component({
  selector: 'app-calendar',
  imports: [
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTooltipModule
  ],
  templateUrl: './calendar.html',
  styleUrl: './calendar.scss',
})
export class Calendar {
  private readonly tasksService = inject(TasksService);
  private readonly holidaysService = inject(HolidaysService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly month = signal(new Date());
  protected readonly tasks = signal<TaskDto[]>([]);
  protected readonly holidays = signal<HolidayDto[]>([]);

  protected readonly monthLabel = computed(() =>
    this.month().toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' })
  );

  protected readonly weekDays = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

  protected readonly days = computed<CalendarDay[]>(() => {
    const first = new Date(this.month().getFullYear(), this.month().getMonth(), 1);
    const mondayOffset = (first.getDay() + 6) % 7;
    const gridStart = new Date(first);
    gridStart.setDate(first.getDate() - mondayOffset);
    const today = new Date();
    const byDate = new Map<string, TaskDto[]>();
    for (const task of this.tasks()) {
      if (task.dueDate) {
        const key = task.dueDate;
        byDate.set(key, [...(byDate.get(key) ?? []), task]);
      }
    }
    const holidayByDate = new Map<string, HolidayDto[]>();
    for (const holiday of this.holidays()) {
      const key = holiday.date;
      holidayByDate.set(key, [...(holidayByDate.get(key) ?? []), holiday]);
    }
    const result: CalendarDay[] = [];
    for (let i = 0; i < 42; i++) {
      const date = new Date(gridStart);
      date.setDate(gridStart.getDate() + i);
      const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
      result.push({
        date,
        inMonth: date.getMonth() === this.month().getMonth(),
        isToday: date.toDateString() === today.toDateString(),
        tasks: byDate.get(key) ?? [],
        holidays: holidayByDate.get(key) ?? []
      });
    }
    return result;
  });

  protected readonly priorityColors: Record<string, 'primary' | 'accent' | 'warn'> = {
    LOW: 'primary',
    MEDIUM: 'accent',
    HIGH: 'accent',
    URGENT: 'warn'
  };

  constructor() {
    this.load();
  }

  protected previousMonth(): void {
    const d = new Date(this.month());
    d.setMonth(d.getMonth() - 1);
    this.month.set(d);
    this.load();
  }

  protected nextMonth(): void {
    const d = new Date(this.month());
    d.setMonth(d.getMonth() + 1);
    this.month.set(d);
    this.load();
  }

  protected goToToday(): void {
    this.month.set(new Date());
    this.load();
  }

  private load(): void {
    const year = this.month().getFullYear();
    const month = this.month().getMonth();
    const first = new Date(year, month, 1);
    const mondayOffset = (first.getDay() + 6) % 7;
    const gridStart = new Date(first);
    gridStart.setDate(first.getDate() - mondayOffset);
    const gridEnd = new Date(gridStart);
    gridEnd.setDate(gridStart.getDate() + 41);
    const iso = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    this.tasksService.list(undefined, undefined, undefined, undefined, undefined, iso(gridStart), iso(gridEnd), 0, 500)
      .subscribe({
        next: page => this.tasks.set(page.content),
        error: () => this.snackBar.open('Impossible de charger le calendrier', 'Fermer', { duration: 4000 })
      });
    const years = new Set<number>([gridStart.getFullYear(), gridEnd.getFullYear()]);
    years.forEach(y =>
      this.holidaysService.list(y).subscribe({
        next: holidays => {
          const merged = new Map(this.holidays().map(h => [h.date, h]));
          for (const h of holidays) {
            merged.set(h.date, h);
          }
          this.holidays.set([...merged.values()]);
        }
      })
    );
  }
}
