import { ActivatedRouteSnapshot, CanActivateFn, RouterStateSnapshot } from '@angular/router';
import { AuthGuardData, createAuthGuard } from 'keycloak-angular';

/**
 * Protege rotas que exigem apenas usuário logado (sem checar role), como
 * /chat. Distinto do adminGuard, que também checa a role ADMIN.
 *
 * Exportado separado de `authGuard` como costura de testabilidade: testar
 * este predicado com um AuthGuardData construído à mão é bem mais simples
 * do que acionar a resolução de DI interna do createAuthGuard.
 */
export const isChatAccessAllowed = async (
  _route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  authData: AuthGuardData
): Promise<boolean> => {
  if (authData.authenticated) {
    return true;
  }

  await authData.keycloak.login({ redirectUri: window.location.origin + state.url });
  return false;
};

export const authGuard = createAuthGuard<CanActivateFn>(isChatAccessAllowed);
