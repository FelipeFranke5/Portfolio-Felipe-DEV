import { HttpInterceptorFn } from '@angular/common/http';
import { includeBearerTokenInterceptor } from 'keycloak-angular';

/**
 * Injeta o token Bearer nas requisições autenticadas, conforme descrito no
 * fluxo de autenticação do README.md (passo 4).
 *
 * Delega para o includeBearerTokenInterceptor do keycloak-angular em vez de
 * reimplementar: ele já resolve refresh do token + anexação do header,
 * condicionado à config em INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG (ver
 * app.config.ts).
 */
export const authInterceptor: HttpInterceptorFn = includeBearerTokenInterceptor;
