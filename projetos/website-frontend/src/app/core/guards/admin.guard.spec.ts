import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthGuardData } from 'keycloak-angular';

import { isAdminAccessAllowed } from './admin.guard';

describe('isAdminAccessAllowed', () => {
  const route = {} as ActivatedRouteSnapshot;
  const deniedRedirect = { toString: () => '/' } as UrlTree;

  function buildAuthData(
    authenticated: boolean,
    realmRoles: string[],
    loginSpy: ReturnType<typeof vi.fn>
  ): AuthGuardData {
    return {
      authenticated,
      grantedRoles: { realmRoles, resourceRoles: {} },
      keycloak: { login: loginSpy } as unknown as AuthGuardData['keycloak'],
    };
  }

  it('should allow access when the user is authenticated and has the ADMIN realm role', async () => {
    const loginSpy = vi.fn();
    const authData = buildAuthData(true, ['ADMIN'], loginSpy);
    const state = { url: '/admin' } as RouterStateSnapshot;

    const result = await isAdminAccessAllowed(route, state, authData, deniedRedirect);

    expect(result).toBe(true);
    expect(loginSpy).not.toHaveBeenCalled();
  });

  it('should redirect to Keycloak login and deny access when the user is not authenticated', async () => {
    const loginSpy = vi.fn().mockResolvedValue(undefined);
    const authData = buildAuthData(false, [], loginSpy);
    const state = { url: '/admin' } as RouterStateSnapshot;

    const result = await isAdminAccessAllowed(route, state, authData, deniedRedirect);

    expect(result).toBe(false);
    expect(loginSpy).toHaveBeenCalledWith({
      redirectUri: window.location.origin + '/admin',
    });
  });

  it('should redirect home WITHOUT calling login when authenticated but missing the ADMIN role', async () => {
    const loginSpy = vi.fn();
    const authData = buildAuthData(true, ['USER'], loginSpy);
    const state = { url: '/admin' } as RouterStateSnapshot;

    const result = await isAdminAccessAllowed(route, state, authData, deniedRedirect);

    expect(result).toBe(deniedRedirect);
    expect(loginSpy).not.toHaveBeenCalled();
  });
});
