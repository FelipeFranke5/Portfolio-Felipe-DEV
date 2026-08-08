import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/services/api.service';

/**
 * Registro do Log Interno. O controller devolve a entidade JPA direto, sem DTO
 * intermediário — este é o formato exato do JSON.
 */
export interface InternalLog {
  id: string;
  simpleClassName: string;
  errorMessage: string;
  stackTrace: string;
  createdAt: string;
}

/**
 * Teto que o back-end aplica na listagem (`LIMIT 100`, ordenado por
 * `created_at DESC`). Quando vêm exatamente 100 registros não dá para saber se
 * existem mais, então a interface mostra "100+" em vez de fingir precisão.
 */
export const INTERNAL_LOG_PAGE_LIMIT = 100;

/** Idade em que o job agendado do back-end apaga os registros. */
export const INTERNAL_LOG_RETENTION_DAYS = 90;

/**
 * Acesso ao Log Interno (`/api/internal_log`), exclusivo do painel /admin —
 * os dois endpoints exigem ROLE_ADMIN.
 *
 * Somente leitura de propósito: o back-end não expõe POST, PUT nem DELETE
 * para este recurso. Os registros nascem do
 * `GlobalBehaviourExceptionHandler` e morrem no job de retenção.
 */
@Injectable({ providedIn: 'root' })
export class InternalLogsService {
  private readonly apiService = inject(ApiService);

  getLogs() {
    return this.apiService.get<InternalLog[]>('/internal_log', { skipGlobalLoading: true });
  }

  getLogById(id: string) {
    return this.apiService.get<InternalLog>(`/internal_log/${id}`, { skipGlobalLoading: true });
  }
}
