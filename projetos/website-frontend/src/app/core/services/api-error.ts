import { HttpErrorResponse } from '@angular/common/http';

/**
 * Tradução das duas formas de erro que o back-end usa, descritas no README.md
 * ("Padrão de resposta de erro"), para algo consumível pela interface.
 *
 * São funções puras em vez de um serviço injetável porque não dependem de nada
 * do Angular: assim os componentes e os testes as chamam diretamente, sem
 * precisar de TestBed.
 */

/** Corpo de 400/404/500 (`GlobalBehaviourExceptionHandler`). */
interface ReasonErrorBody {
  error?: string;
  reason?: string;
}

/** Corpo de 422 (`UnprocessableEntityResponse`). */
interface ValidationErrorBody {
  message?: string;
  errors?: Record<string, string[]>[];
}

export const VALIDATION_STATUS = 422;

/**
 * Achata o `errors` do 422 em um mapa campo -> mensagens.
 *
 * O back-end devolve uma LISTA de objetos de um único campo cada
 * (`[{ "name": ["..."] }, { "stack": ["..."] }]`), e não um objeto plano — ver
 * `GlobalBehaviourExceptionHandler.toErrorList`. Um mesmo campo também pode
 * aparecer com várias mensagens (`@NotBlank` e `@Size` disparam juntos), por
 * isso as listas são concatenadas em vez de sobrescritas.
 */
export function flattenValidationErrors(error: HttpErrorResponse): Record<string, string[]> {
  if (error.status !== VALIDATION_STATUS) {
    return {};
  }

  const body = error.error as ValidationErrorBody | null;
  const flattened: Record<string, string[]> = {};

  for (const entry of body?.errors ?? []) {
    for (const [field, messages] of Object.entries(entry ?? {})) {
      flattened[field] = [...(flattened[field] ?? []), ...messages];
    }
  }

  return flattened;
}

/**
 * Mensagem legível para exibir ao usuário.
 *
 * O 500 é o caso mais importante: o `reason` carrega o ID do registro de Log
 * Interno gerado para aquela falha, que é justamente o que o administrador
 * precisa para investigar em /admin/logs.
 */
export function resolveApiErrorMessage(error: HttpErrorResponse, fallback: string): string {
  if (error.status === 0) {
    return 'Não foi possível falar com o servidor. Verifique sua conexão e tente novamente.';
  }

  if (error.status === VALIDATION_STATUS) {
    const messages = Object.values(flattenValidationErrors(error)).flat();
    return messages.length > 0 ? messages.join(' ') : fallback;
  }

  const body = error.error as ReasonErrorBody | null;
  return body?.reason ?? body?.error ?? fallback;
}

/**
 * Sessão inválida do ponto de vista do back-end: 401 (sem token ou token
 * expirado) e 403 (token válido, mas sem a role ADMIN). Nos dois casos o
 * painel para de tentar e pede autenticação de novo.
 */
export function isAuthError(error: HttpErrorResponse): boolean {
  return error.status === 401 || error.status === 403;
}
