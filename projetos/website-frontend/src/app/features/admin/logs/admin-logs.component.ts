import {
  Component,
  ElementRef,
  HostListener,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';

import {
  INTERNAL_LOG_PAGE_LIMIT,
  INTERNAL_LOG_RETENTION_DAYS,
  InternalLog,
  InternalLogsService,
} from './internal-logs.service';
import { SessionIdleService } from '../../../core/services/session-idle.service';
import { isAuthError, resolveApiErrorMessage } from '../../../core/services/api-error';

/**
 * Tela somente leitura: o back-end expõe apenas `GET /api/internal_log` e
 * `GET /api/internal_log/{id}`. Não existe criação, edição nem exclusão de
 * registro — eles nascem do handler global de exceções e são apagados pelo job
 * de retenção.
 */
@Component({
  selector: 'app-admin-logs',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './admin-logs.component.html',
  styleUrl: './admin-logs.component.scss',
})
export class AdminLogsComponent {
  private readonly internalLogsService = inject(InternalLogsService);
  private readonly sessionIdle = inject(SessionIdleService);
  private readonly route = inject(ActivatedRoute);

  private readonly listErrorMessage = 'Não foi possível carregar os logs. Tente novamente.';
  private readonly detailErrorMessage =
    'Não foi possível carregar o detalhe deste log. Tente novamente.';

  readonly pageLimit = INTERNAL_LOG_PAGE_LIMIT;
  readonly retentionDays = INTERNAL_LOG_RETENTION_DAYS;

  readonly logs = signal<InternalLog[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly search = signal('');

  readonly activeLog = signal<InternalLog | null>(null);
  readonly isModalOpen = signal(false);
  readonly isModalLoading = signal(false);
  readonly modalErrorMessage = signal<string | null>(null);
  readonly copyFeedback = signal<string | null>(null);

  private readonly searchInput = viewChild<ElementRef<HTMLInputElement>>('searchInput');
  private readonly closeButton = viewChild<ElementRef<HTMLButtonElement>>('closeButton');

  private lastFocusedElement: HTMLElement | null = null;
  private copyFeedbackTimeout?: ReturnType<typeof setTimeout>;

  readonly filteredLogs = computed(() => {
    const search = this.search().trim().toLowerCase();

    if (!search) {
      return this.logs();
    }

    return this.logs().filter(
      (log) =>
        log.simpleClassName.toLowerCase().includes(search) ||
        log.errorMessage?.toLowerCase().includes(search)
    );
  });

  readonly totalResults = computed(() => this.filteredLogs().length);
  readonly hasResults = computed(() => this.totalResults() > 0);
  readonly isListCapped = computed(() => this.logs().length >= INTERNAL_LOG_PAGE_LIMIT);

  /** Classes de exceção mais frequentes na janela carregada. */
  readonly topExceptions = computed(() => {
    const counts = new Map<string, number>();

    for (const log of this.logs()) {
      counts.set(log.simpleClassName, (counts.get(log.simpleClassName) ?? 0) + 1);
    }

    return Array.from(counts.entries())
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 4);
  });

  constructor() {
    this.fetchLogs();

    // O dashboard oferece um atalho "Pesquisar Logs" que já chega com o campo
    // de busca pronto para digitar.
    if (this.route.snapshot.queryParamMap.get('focus') === 'search') {
      effect(() => {
        this.searchInput()?.nativeElement.focus({ preventScroll: true });
      });
    }

    effect(() => {
      if (this.isModalOpen()) {
        this.closeButton()?.nativeElement.focus();
      }
    });
  }

  @HostListener('document:keydown.escape')
  onEscapePressed(): void {
    if (this.isModalOpen()) {
      this.closeModal();
    }
  }

  fetchLogs(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.internalLogsService.getLogs().subscribe({
      next: (logs) => {
        this.logs.set(logs);
        this.isLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading.set(false);

        if (isAuthError(error)) {
          this.sessionIdle.markExpired();
        }

        this.errorMessage.set(resolveApiErrorMessage(error, this.listErrorMessage));
      },
    });
  }

  openLog(log: InternalLog, trigger: HTMLElement): void {
    this.lastFocusedElement = trigger;
    this.activeLog.set(null);
    this.copyFeedback.set(null);
    this.isModalOpen.set(true);
    this.isModalLoading.set(true);
    this.modalErrorMessage.set(null);

    // A listagem já traz o stack trace, mas o detalhe é recarregado por ID de
    // propósito: é o mesmo endpoint que o `reason` de um erro 500 manda o
    // administrador consultar, então vale exercitá-lo aqui.
    this.internalLogsService.getLogById(log.id).subscribe({
      next: (detail) => {
        this.activeLog.set(detail);
        this.isModalLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.isModalLoading.set(false);

        if (isAuthError(error)) {
          this.sessionIdle.markExpired();
        }

        this.modalErrorMessage.set(resolveApiErrorMessage(error, this.detailErrorMessage));
      },
    });
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.activeLog.set(null);
    this.modalErrorMessage.set(null);
    this.copyFeedback.set(null);
    this.lastFocusedElement?.focus();
    this.lastFocusedElement = null;
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeModal();
    }
  }

  copyStackTrace(): void {
    const log = this.activeLog();

    if (!log?.stackTrace) {
      return;
    }

    clearTimeout(this.copyFeedbackTimeout);

    navigator.clipboard
      .writeText(log.stackTrace)
      .then(() => this.copyFeedback.set('Stack trace copiado.'))
      .catch(() => this.copyFeedback.set('Não foi possível copiar automaticamente.'));

    this.copyFeedbackTimeout = setTimeout(() => this.copyFeedback.set(null), 3000);
  }

  clearSearch(): void {
    this.search.set('');
  }
}
