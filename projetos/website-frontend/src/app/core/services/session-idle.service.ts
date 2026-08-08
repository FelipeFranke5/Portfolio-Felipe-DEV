import { DOCUMENT, Injectable, inject, signal } from '@angular/core';

/** Tempo sem interação a partir do qual a sessão administrativa é considerada expirada. */
export const IDLE_TIMEOUT_MS = 15 * 60 * 1000;

/** Frequência com que o relógio confere o tempo ocioso. */
const IDLE_CHECK_INTERVAL_MS = 15 * 1000;

/**
 * Registrar cada `mousemove` seria caro (dezenas de eventos por segundo só para
 * gravar um timestamp), então a atividade é anotada no máximo uma vez por
 * segundo. A precisão perdida é irrelevante diante de um timeout de minutos.
 */
const ACTIVITY_THROTTLE_MS = 1000;

const ACTIVITY_EVENTS = ['mousemove', 'mousedown', 'keydown', 'scroll', 'touchstart'] as const;

/**
 * Detecta a inatividade do administrador no painel /admin, para o estado
 * "Sessão inativa" previsto no design.
 *
 * Só roda enquanto o shell do /admin estiver montado (`start()` no construtor
 * dele, `stop()` no destroy): as páginas públicas do site não têm sessão para
 * expirar, e um timer global ficaria ligado à toa.
 *
 * A expiração também pode ser sinalizada de fora por `markExpired()` — é o que
 * as telas fazem ao receber 401/403 da API, casos em que o token já morreu
 * antes de o tempo ocioso estourar.
 */
@Injectable({ providedIn: 'root' })
export class SessionIdleService {
  private readonly document = inject(DOCUMENT);

  private readonly _isExpired = signal(false);

  /** Somente leitura para quem consome (ex.: AdminShellComponent). */
  readonly isExpired = this._isExpired.asReadonly();

  private lastActivityAt = 0;
  private lastRecordedAt = 0;
  private intervalId?: ReturnType<typeof setInterval>;

  private readonly onActivity = (): void => {
    const now = Date.now();

    if (now - this.lastRecordedAt < ACTIVITY_THROTTLE_MS) {
      return;
    }

    this.lastRecordedAt = now;
    this.lastActivityAt = now;
  };

  start(): void {
    if (this.intervalId !== undefined) {
      return;
    }

    this._isExpired.set(false);
    this.lastActivityAt = Date.now();
    this.lastRecordedAt = 0;

    for (const eventName of ACTIVITY_EVENTS) {
      this.document.addEventListener(eventName, this.onActivity, { passive: true });
    }

    this.intervalId = setInterval(() => this.checkIdle(), IDLE_CHECK_INTERVAL_MS);
  }

  stop(): void {
    if (this.intervalId === undefined) {
      return;
    }

    clearInterval(this.intervalId);
    this.intervalId = undefined;

    for (const eventName of ACTIVITY_EVENTS) {
      this.document.removeEventListener(eventName, this.onActivity);
    }
  }

  /** Marca a sessão como expirada sem esperar o tempo ocioso (401/403 da API). */
  markExpired(): void {
    this._isExpired.set(true);
  }

  private checkIdle(): void {
    if (this._isExpired()) {
      return;
    }

    if (Date.now() - this.lastActivityAt >= IDLE_TIMEOUT_MS) {
      this.markExpired();
    }
  }
}
