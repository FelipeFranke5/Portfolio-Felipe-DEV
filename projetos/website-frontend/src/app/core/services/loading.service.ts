import { Injectable, signal } from '@angular/core';

/**
 * Controla o estado global de "carregando" da aplicação.
 *
 * Usa um CONTADOR de requisições em andamento em vez de um booleano simples,
 * para suportar chamadas concorrentes: se duas requisições HTTP estiverem em
 * voo ao mesmo tempo, o overlay só pode sumir quando a ÚLTIMA delas terminar
 * (sucesso ou erro). Um booleano ingênuo (`isLoading = false` direto no
 * `finalize` da primeira que terminar) esconderia a animação enquanto a
 * segunda requisição ainda está pendente — um bug sutil e fácil de não notar
 * em testes manuais com uma única chamada por vez.
 *
 * Alimentado automaticamente pelo `loadingInterceptor`
 * (core/interceptors/loading.interceptor.ts) a cada requisição HTTP, mas
 * também pode ser chamado manualmente por um serviço/componente que precise
 * sinalizar um carregamento que não passe pelo HttpClient.
 */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private pendingRequests = 0;

  private readonly _isLoading = signal(false);

  /** Somente leitura para quem consome (ex.: LoadingOverlayComponent). */
  readonly isLoading = this._isLoading.asReadonly();

  show(): void {
    this.pendingRequests++;
    this._isLoading.set(true);
  }

  hide(): void {
    // Math.max evita o contador ficar negativo caso hide() seja chamado
    // mais vezes que show() por algum erro de uso futuro.
    this.pendingRequests = Math.max(0, this.pendingRequests - 1);

    if (this.pendingRequests === 0) {
      this._isLoading.set(false);
    }
  }
}
