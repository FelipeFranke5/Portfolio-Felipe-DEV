import { Routes } from '@angular/router';

/**
 * As quatro telas são filhas do AdminShellComponent para compartilharem a
 * moldura (header, subnav, footer) e o relógio de inatividade sem remontá-los a
 * cada navegação. O `adminGuard` continua aplicado no pai `/admin`
 * (app.routes.ts) e cobre todas as filhas.
 */
export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./admin-shell.component').then((m) => m.AdminShellComponent),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./admin-dashboard.component').then((m) => m.AdminDashboardComponent),
      },
      {
        path: 'projects',
        loadComponent: () =>
          import('./projects/admin-projects.component').then((m) => m.AdminProjectsComponent),
      },
      {
        path: 'skills',
        loadComponent: () =>
          import('./skills/admin-skills.component').then((m) => m.AdminSkillsComponent),
      },
      {
        path: 'logs',
        loadComponent: () =>
          import('./logs/admin-logs.component').then((m) => m.AdminLogsComponent),
      },
    ],
  },
];
