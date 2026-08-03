import { ActivatedRouteSnapshot, CanActivateFn, RouterStateSnapshot } from '@angular/router';
import { AuthGuardData, createAuthGuard } from 'keycloak-angular';

/**
 * Protege a rota /admin: exige usuário logado E a role ADMIN (role de realm
 * no Keycloak, conferida em SecurityConfig no back-end). Distinto do
 * authGuard, que só exige login.
 *
 * Exportado separado de `adminGuard` pelo mesmo motivo do authGuard: testar
 * este predicado com um AuthGuardData construído à mão é bem mais simples
 * do que acionar a resolução de DI interna do createAuthGuard.
 */
export const isAdminAccessAllowed = async (
  _route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  authData: AuthGuardData
): Promise<boolean> => {
  if (authData.authenticated && authData.grantedRoles.realmRoles.includes('ADMIN')) {
    return true;
  }

  // Só redireciona para o login quando não há sessão nenhuma. Um usuário já
  // autenticado mas sem a role ADMIN chamaria login() de novo, o Keycloak
  // devolveria o SSO existente com o mesmo token (ainda sem a role), e o
  // guard bloquearia de novo — um loop de redirect. Nesse caso só nega.
  if (!authData.authenticated) {
    await authData.keycloak.login({ redirectUri: window.location.origin + state.url });
  }

  return false;
};

export const adminGuard = createAuthGuard<CanActivateFn>(isAdminAccessAllowed);
