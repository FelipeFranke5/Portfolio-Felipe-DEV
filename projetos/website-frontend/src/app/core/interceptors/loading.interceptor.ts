import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';

import { LoadingService } from '../services/loading.service';

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
  const loadingService = inject(LoadingService);
  loadingService.show();

  return next(req).pipe(finalize(() => loadingService.hide()));
};
