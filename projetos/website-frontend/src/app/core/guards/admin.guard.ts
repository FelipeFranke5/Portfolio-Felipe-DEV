import { inject } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { AuthGuardData, createAuthGuard } from 'keycloak-angular';

/**
 * Protege a rota /admin: exige usuário logado E a role ADMIN (role de realm
 * no Keycloak, conferida em SecurityConfig no back-end). Distinto do
 * authGuard, que só exige login.
 *
 * Exportado separado de `adminGuard` pelo mesmo motivo do authGuard: testar
 * este predicado com um AuthGuardData construído à mão é bem mais simples
 * do que acionar a resolução de DI interna do createAuthGuard. O destino de
 * recusa entra como parâmetro em vez de ser resolvido aqui dentro para manter
 * essa propriedade — quem monta o UrlTree é o `adminGuard`, no contexto de DI.
 */
export const isAdminAccessAllowed = async (
  _route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  authData: AuthGuardData,
  deniedRedirect: UrlTree
): Promise<boolean | UrlTree> => {
  if (authData.authenticated && authData.grantedRoles.realmRoles.includes('ADMIN')) {
    return true;
  }

  // Só redireciona para o login quando não há sessão nenhuma. Um usuário já
  // autenticado mas sem a role ADMIN chamaria login() de novo, o Keycloak
  // devolveria o SSO existente com o mesmo token (ainda sem a role), e o
  // guard bloquearia de novo — um loop de redirect.
  if (!authData.authenticated) {
    await authData.keycloak.login({ redirectUri: window.location.origin + state.url });
    return false;
  }

  // Autenticado, mas sem a role: devolver `false` puro deixaria o usuário na
  // página anterior — ou em uma tela em branco, no acesso direto por URL, já
  // que não houve navegação anterior nenhuma. Mandar para a home é a única
  // saída que dá algum retorno visual ao usuário sem cair no loop acima.
  return deniedRedirect;
};

export const adminGuard: CanActivateFn = (route, state) => {
  const deniedRedirect = inject(Router).parseUrl('/');

  return createAuthGuard<CanActivateFn>((currentRoute, currentState, authData) =>
    isAdminAccessAllowed(currentRoute, currentState, authData, deniedRedirect)
  )(route, state);
};
