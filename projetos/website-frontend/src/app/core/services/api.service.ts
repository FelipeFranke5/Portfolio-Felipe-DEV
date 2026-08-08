import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { SKIP_GLOBAL_LOADING } from '../interceptors/loading.interceptor';

/**
 * Opções por requisição. Hoje só existe `skipGlobalLoading`, para as telas que
 * exibem o próprio estado de carregamento (ver SKIP_GLOBAL_LOADING).
 */
export interface ApiRequestOptions {
  skipGlobalLoading?: boolean;
}

function buildContext(options?: ApiRequestOptions): HttpContext | undefined {
  if (!options?.skipGlobalLoading) {
    return undefined;
  }
  return new HttpContext().set(SKIP_GLOBAL_LOADING, true);
}

/**
 * Cliente HTTP de baixo nível, centraliza a base da API (proxy NGINX -> Spring Boot em /api/*,
 * conforme ARCHITECTURE.md). Os services de cada feature devem consumir este serviço em vez
 * de usar o HttpClient diretamente, mantendo a URL base em um único lugar.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  get<T>(path: string, options?: ApiRequestOptions) {
    return this.http.get<T>(`${this.baseUrl}${path}`, { context: buildContext(options) });
  }

  post<T>(path: string, body: unknown, options?: ApiRequestOptions) {
    return this.http.post<T>(`${this.baseUrl}${path}`, body, { context: buildContext(options) });
  }

  put<T>(path: string, body: unknown, options?: ApiRequestOptions) {
    return this.http.put<T>(`${this.baseUrl}${path}`, body, { context: buildContext(options) });
  }

  delete<T>(path: string, options?: ApiRequestOptions) {
    return this.http.delete<T>(`${this.baseUrl}${path}`, { context: buildContext(options) });
  }
}
