import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  IncludeBearerTokenCondition,
  provideKeycloak,
  withAutoRefreshToken,
} from 'keycloak-angular';

import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { loadingInterceptor } from './core/interceptors/loading.interceptor';

// O Bearer token só deve ir para o próprio backend (environment.apiUrl), nunca
// para o handshake do chat (/api/websocket) — que usa SockJS/XHR bruto e por
// isso nunca passaria por este interceptor de qualquer forma (HttpInterceptorFn
// só enxerga tráfego emitido pelo HttpClient). A exclusão fica aqui só para
// documentar a intenção para quem ler depois.
const bearerTokenConditions: IncludeBearerTokenCondition[] = [
  {
    urlPattern: new RegExp(
      `^${environment.apiUrl.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}(?!/websocket)`
    ),
  },
];

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor, loadingInterceptor])),
    {
      provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
      useValue: bearerTokenConditions,
    },
    provideKeycloak({
      config: environment.keycloak,
      initOptions: {
        // 'check-sso' (não 'login-required'): a maioria das rotas é pública,
        // então só detectamos silenciosamente uma sessão SSO já existente,
        // sem forçar redirect de login em toda a aplicação.
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        pkceMethod: 'S256',
      },
      features: [withAutoRefreshToken({ onInactivityTimeout: 'none' })],
    }),
  ],
};
