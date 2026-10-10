import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { TaskDialogComponent } from './task-dialog';
import { TasksService } from '../../core/services/tasks.service';
import { EmployeesService } from '../../core/services/employees.service';
import { SkillsService } from '../../core/services/skills.service';
import { RecommendationDto } from '../../core/models/task.model';

function recommendation(employeeId: number, totalScore: number): RecommendationDto {
  return {
    employeeId,
    employeeName: `Employé ${employeeId}`,
    totalScore,
    criteria: [
      { name: 'skills', score: 0.5, explanation: '1/2 compétences couvertes' },
      { name: 'workload', score: 1, explanation: 'aucune tâche en cours' }
    ],
    explanation: 'Profil équilibré'
  };
}

describe('TaskDialogComponent (recommandation IA)', () => {
  let tasksMock: {
    recommend: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let snackBarMock: { open: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    tasksMock = {
      recommend: vi.fn().mockReturnValue(of([recommendation(1, 0.8), recommendation(2, 0.4)])),
      create: vi.fn().mockReturnValue(of({})),
      update: vi.fn().mockReturnValue(of({}))
    };
    snackBarMock = { open: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [TaskDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: MatDialogRef, useValue: { close: vi.fn() } },
        { provide: MAT_DIALOG_DATA, useValue: { task: null, projects: [] } },
        { provide: MatSnackBar, useValue: snackBarMock },
        { provide: TasksService, useValue: tasksMock },
        { provide: EmployeesService, useValue: { list: () => of({ content: [] }) } },
        { provide: SkillsService, useValue: { list: () => of({ content: [] }) } }
      ]
    }).compileComponents();
  });

  it('appelle le service et affiche les profils recommandés', () => {
    const fixture = TestBed.createComponent(TaskDialogComponent);
    const component = fixture.componentInstance as any;
    component.form.patchValue({ skillIds: [1, 2], priority: 'HIGH' });

    component.recommend();

    expect(tasksMock.recommend).toHaveBeenCalledWith([1, 2], undefined, undefined, 'HIGH');
    expect(component.recommending()).toBe(false);
    expect(component.recommendations().length).toBe(2);
    expect(component.recommendations()[0].employeeId).toBe(1);
  });

  it('limite la liste aux cinq meilleurs profils', () => {
    tasksMock.recommend.mockReturnValue(of([1, 2, 3, 4, 5, 6].map(id => recommendation(id, 1 - id / 10))));
    const fixture = TestBed.createComponent(TaskDialogComponent);
    const component = fixture.componentInstance as any;

    component.recommend();

    expect(component.recommendations().length).toBe(5);
  });

  it('signale une erreur et arrête le chargement', () => {
    tasksMock.recommend.mockReturnValue({ subscribe: (observer: any) => observer.error(new Error('boom')) });
    const fixture = TestBed.createComponent(TaskDialogComponent);
    const component = fixture.componentInstance as any;

    component.recommend();

    expect(component.recommending()).toBe(false);
    expect(snackBarMock.open).toHaveBeenCalled();
  });

  it('assigne l\'employé choisi et masque les suggestions', () => {
    const fixture = TestBed.createComponent(TaskDialogComponent);
    const component = fixture.componentInstance as any;
    component.recommend();

    component.choose(7);

    expect(component.form.controls.assigneeId.value).toBe(7);
    expect(component.recommendations().length).toBe(0);
  });

  it('traduit les noms de critères', () => {
    const fixture = TestBed.createComponent(TaskDialogComponent);
    const component = fixture.componentInstance as any;

    expect(component.criterionLabel('skills')).toBe('Compétences');
    expect(component.criterionLabel('reliability')).toBe('Fiabilité');
    expect(component.criterionLabel('inconnu')).toBe('inconnu');
  });
});
