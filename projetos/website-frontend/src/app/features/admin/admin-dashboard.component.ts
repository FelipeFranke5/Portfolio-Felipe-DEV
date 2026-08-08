import { Component, DOCUMENT, OnDestroy, computed, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { ProjectsService } from '../portfolio/projects.service';
import { SkillsService } from '../skills/skills.service';
import { INTERNAL_LOG_PAGE_LIMIT, InternalLogsService } from './logs/internal-logs.service';
import { SessionIdleService } from '../../core/services/session-idle.service';
import { isAuthError, resolveApiErrorMessage } from '../../core/services/api-error';

type MetricKey = 'projects' | 'skills' | 'logs';

interface MetricCard {
  key: MetricKey;
  tone: string;
  kicker: string;
  title: string;
  description: string;
}

interface DashboardError {
  status: number;
  statusText: string;
  message: string;
}

const COUNT_UP_DURATION_MS = 2000;

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './admin-dashboard.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnDestroy {
  private readonly projectsService = inject(ProjectsService);
  private readonly skillsService = inject(SkillsService);
  private readonly internalLogsService = inject(InternalLogsService);
  private readonly sessionIdle = inject(SessionIdleService);
  private readonly document = inject(DOCUMENT);

  private readonly defaultErrorMessage =
    'Não foi possível carregar os indicadores do painel. Tente novamente.';

  readonly metricCards: MetricCard[] = [
    {
      key: 'projects',
      tone: 'tone-accent',
      kicker: 'Métrica',
      title: 'Projetos',
      description: 'Projetos cadastrados no portfólio.',
    },
    {
      key: 'skills',
      tone: 'tone-success',
      kicker: 'Métrica',
      title: 'Skills',
      description: 'Habilidades técnicas registradas.',
    },
    {
      key: 'logs',
      tone: 'tone-warning',
      kicker: 'Status',
      title: 'Exceções',
      description: 'Erros técnicos não tratados registrados.',
    },
  ];

  readonly isLoading = signal(true);
  readonly error = signal<DashboardError | null>(null);

  /** Valor final vindo da API, por indicador. */
  private readonly targets = signal<Record<MetricKey, number>>({
    projects: 0,
    skills: 0,
    logs: 0,
  });

  /** Valor atualmente exibido — sobe progressivamente até alcançar o alvo. */
  readonly displayedValues = signal<Record<MetricKey, number>>({
    projects: 0,
    skills: 0,
    logs: 0,
  });

  /** Vira `true` quando a contagem termina; dispara a troca de cor do número. */
  readonly isCountUpDone = signal(false);

  /**
   * A listagem de logs tem teto de 100 registros no back-end, então esse valor
   * é um piso, não o total. Exibir "128" como o mockup faz seria inventar
   * precisão que a API não dá.
   */
  readonly isLogCountCapped = computed(() => this.targets().logs >= INTERNAL_LOG_PAGE_LIMIT);

  private animationFrameId?: number;

  constructor() {
    this.fetchMetrics();
  }

  ngOnDestroy(): void {
    this.cancelCountUp();
  }

  fetchMetrics(): void {
    this.cancelCountUp();
    this.isLoading.set(true);
    this.isCountUpDone.set(false);
    this.error.set(null);
    this.displayedValues.set({ projects: 0, skills: 0, logs: 0 });

    forkJoin({
      projects: this.projectsService.getProjectsForAdmin(),
      skills: this.skillsService.getSkillsForAdmin(),
      logs: this.internalLogsService.getLogs(),
    }).subscribe({
      next: ({ projects, skills, logs }) => {
        this.targets.set({
          projects: projects.length,
          skills: skills.length,
          logs: logs.length,
        });
        this.isLoading.set(false);
        this.startCountUp();
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading.set(false);

        if (isAuthError(error)) {
          this.sessionIdle.markExpired();
        }

        this.error.set({
          status: error.status,
          statusText: error.statusText || 'Erro',
          message: resolveApiErrorMessage(error, this.defaultErrorMessage),
        });
      },
    });
  }

  displayedValue(key: MetricKey): string {
    const value = this.displayedValues()[key];

    if (key === 'logs' && this.isCountUpDone() && this.isLogCountCapped()) {
      return `${value}+`;
    }

    return String(value);
  }

  /**
   * Contagem crescente do design. Interpola os três indicadores no mesmo
   * `requestAnimationFrame` em vez de um por card, para eles terminarem juntos
   * e para não concorrerem por frames.
   */
  private startCountUp(): void {
    if (this.prefersReducedMotion()) {
      this.displayedValues.set({ ...this.targets() });
      this.isCountUpDone.set(true);
      return;
    }

    const targets = this.targets();
    let startTimestamp: number | null = null;

    const step = (timestamp: number): void => {
      startTimestamp ??= timestamp;
      const progress = Math.min((timestamp - startTimestamp) / COUNT_UP_DURATION_MS, 1);

      this.displayedValues.set({
        projects: Math.floor(progress * targets.projects),
        skills: Math.floor(progress * targets.skills),
        logs: Math.floor(progress * targets.logs),
      });

      if (progress < 1) {
        this.animationFrameId = requestAnimationFrame(step);
        return;
      }

      this.animationFrameId = undefined;
      this.displayedValues.set({ ...targets });
      this.isCountUpDone.set(true);
    };

    this.animationFrameId = requestAnimationFrame(step);
  }

  private cancelCountUp(): void {
    if (this.animationFrameId !== undefined) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = undefined;
    }
  }

  private prefersReducedMotion(): boolean {
    return (
      this.document.defaultView?.matchMedia('(prefers-reduced-motion: reduce)').matches ?? false
    );
  }
}
