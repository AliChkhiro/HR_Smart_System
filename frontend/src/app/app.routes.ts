import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { MainLayout } from './layout/main-layout/main-layout';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/auth/login/login').then(m => m.Login) },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard) },
      { path: 'projects', loadComponent: () => import('./features/projects/projects').then(m => m.Projects) },
      { path: 'tasks', loadComponent: () => import('./features/tasks/tasks').then(m => m.Tasks) },
      { path: 'leaves', loadComponent: () => import('./features/leaves/leaves').then(m => m.Leaves) },
      { path: 'employees', loadComponent: () => import('./features/employees/employees').then(m => m.Employees) },
      { path: 'departments', loadComponent: () => import('./features/departments/departments').then(m => m.Departments) },
      { path: 'users', loadComponent: () => import('./features/users/users').then(m => m.Users) },
      { path: 'skills', loadComponent: () => import('./features/skills/skills').then(m => m.Skills) },
      { path: 'calendar', loadComponent: () => import('./features/calendar/calendar').then(m => m.Calendar) },
      { path: 'notifications', loadComponent: () => import('./features/notifications/notifications').then(m => m.Notifications) },
      { path: 'settings', loadComponent: () => import('./features/settings/settings').then(m => m.Settings) }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
