import { HttpContextToken, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';

import { LoadingService } from '../services/loading.service';

/**
 * Marca uma requisição que NÃO deve acionar o overlay global de carregamento.
 *
 * Existe por causa do painel /admin: as telas de lá têm estado de carregamento
 * próprio e mais informativo (skeleton dos cards, shimmer dos indicadores com
 * contagem crescente, botão em "Salvando…"). Sem isso, o overlay em tela cheia
 * cobriria exatamente a animação que o usuário deveria estar vendo, e piscaria
 * a cada operação de CRUD.
 *
 * O default `false` mantém o comportamento antigo para todo o resto do site.
 */
export const SKIP_GLOBAL_LOADING = new HttpContextToken<boolean>(() => false);

/**
 * Chama `LoadingService.show()` antes de disparar cada requisição HTTP e
 * `hide()` quando ela finaliza — em sucesso OU erro, por isso o uso de
 * `finalize` em vez de encadear em `tap`/`map` (que só rodam no caminho de
 * sucesso e deixariam o overlay preso na tela para sempre em qualquer
 * requisição que falhe).
 *
 * O `LoadingOverlayComponent`, registrado uma única vez em `AppComponent`,
 * reage a esse estado e exibe a animação de carregamento em tela cheia.
 */
export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.context.get(SKIP_GLOBAL_LOADING)) {
    return next(req);
  }

  const loadingService = inject(LoadingService);
  loadingService.show();

  return next(req).pipe(finalize(() => loadingService.hide()));
};
