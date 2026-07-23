import { Component, inject } from '@angular/core';

import { LoadingService } from '../../../core/services/loading.service';

/**
 * Overlay de carregamento em tela cheia (animação fornecida pelo Design),
 * exibido sempre que `LoadingService.isLoading()` estiver `true` — estado
 * controlado pelo `loadingInterceptor` a cada requisição HTTP em andamento.
 *
 * Componente `shared`, reaproveitável entre features (ver README.md /
 * ARCHITECTURE.md). É registrado UMA ÚNICA VEZ na raiz da aplicação
 * (`AppComponent`, ao lado do `<router-outlet>`), não dentro de cada feature
 * nem dentro de `HeaderComponent`/`FooterComponent`:
 *
 * - Sendo `position: fixed` cobrindo a viewport inteira, o efeito visual é
 *   idêntico a colocá-lo em cada página — mas sem duplicar o import/markup
 *   em 7+ componentes (home, portfolio, skills, contact, admin/*).
 * - Evita o risco de uma feature nova esquecer de incluir o overlay.
 * - Header/Footer nunca precisam saber que ele existe: eles não têm estado
 *   de carregamento próprio.
 */
@Component({
  selector: 'app-loading-overlay',
  standalone: true,
  templateUrl: './loading-overlay.component.html',
  styleUrl: './loading-overlay.component.scss',
})
export class LoadingOverlayComponent {
  protected readonly loadingService = inject(LoadingService);
}
